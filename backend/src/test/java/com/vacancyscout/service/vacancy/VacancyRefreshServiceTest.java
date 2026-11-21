package com.vacancyscout.service.vacancy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vacancyscout.ingestion.RabotaByVacancyDetailFetcher;
import com.vacancyscout.ingestion.RabotaByVacancyDetailFetcher.DetailFetchResult;
import com.vacancyscout.ingestion.RabotaByVacancyDetailFetcher.VacancyDetail;
import com.vacancyscout.model.Vacancy;
import com.vacancyscout.repository.VacancyRepository;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;

class VacancyRefreshServiceTest {

  @Mock private VacancyRepository vacancyRepository;
  @Mock private RabotaByVacancyDetailFetcher fetcher;

  private AutoCloseable mocks;
  private VacancyRefreshService service;

  @BeforeEach
  void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
    service = new VacancyRefreshService(vacancyRepository, fetcher, Duration.ofMillis(2000));
    when(vacancyRepository.updateDetails(any())).thenReturn(Mono.just(true));
    when(vacancyRepository.markChecked(any())).thenReturn(Mono.just(true));
    when(vacancyRepository.deactivate(any())).thenReturn(Mono.just(true));
  }

  @AfterEach
  void tearDown() throws Exception {
    mocks.close();
  }

  @Test
  void refresh_updatesSourceDerivedFields() {
    when(fetcher.fetchWithStatus("https://rabota.by/vacancy/1"))
        .thenReturn(Mono.just(new DetailFetchResult(detail(), false)));

    var outcome = service.refresh(vacancy()).block();

    assertEquals(VacancyRefreshService.Outcome.UPDATED, outcome);
    ArgumentCaptor<Vacancy> captor = ArgumentCaptor.forClass(Vacancy.class);
    verify(vacancyRepository).updateDetails(captor.capture());
    var updated = captor.getValue();
    assertEquals("New title", updated.title());
    assertEquals("new description", updated.description());
    assertEquals("new requirements", updated.requirements());
    assertEquals(List.of("Java"), updated.skills());
    assertEquals("Минск", updated.location());
    assertTrue(updated.isActive());
  }

  @Test
  void refresh_deactivatesVacancyGoneFromSource() {
    var vacancy = vacancy();
    when(fetcher.fetchWithStatus("https://rabota.by/vacancy/1"))
        .thenReturn(Mono.just(new DetailFetchResult(null, true)));

    var outcome = service.refresh(vacancy).block();

    assertEquals(VacancyRefreshService.Outcome.GONE, outcome);
    verify(vacancyRepository).deactivate(vacancy.id());
    verify(vacancyRepository, never()).updateDetails(any());
  }

  @Test
  void refresh_withoutParsedDetailOnlyMarksCheck() {
    var vacancy = vacancy();
    when(fetcher.fetchWithStatus("https://rabota.by/vacancy/1"))
        .thenReturn(Mono.just(new DetailFetchResult(null, false)));

    var outcome = service.refresh(vacancy).block();

    assertEquals(VacancyRefreshService.Outcome.FAILED, outcome);
    verify(vacancyRepository).markChecked(vacancy.id());
    verify(vacancyRepository, never()).deactivate(any());
  }

  @Test
  void refresh_withTimeoutMarksCheck() {
    var vacancy = vacancy();
    when(fetcher.fetchWithStatus("https://rabota.by/vacancy/1")).thenReturn(Mono.never());

    var outcome =
        new VacancyRefreshService(vacancyRepository, fetcher, Duration.ofMillis(50))
            .refresh(vacancy)
            .block();

    assertEquals(VacancyRefreshService.Outcome.FAILED, outcome);
    verify(vacancyRepository).markChecked(vacancy.id());
  }

  @Test
  void refresh_withoutUrlDoesNothing() {
    var noUrl = Vacancy.builder().id(UUID.randomUUID()).url(null).isActive(true).build();

    var outcome = service.refresh(noUrl).block();

    assertEquals(VacancyRefreshService.Outcome.FAILED, outcome);
    verify(fetcher, never()).fetchWithStatus(any());
  }

  private static Vacancy vacancy() {
    return Vacancy.builder()
        .id(UUID.randomUUID())
        .sourceId("1")
        .sourceName("rabota.by")
        .title("Old title")
        .companyName("EPAM Systems")
        .description("")
        .requirements("")
        .responsibilities("")
        .skills(List.of())
        .location("Минск")
        .url("https://rabota.by/vacancy/1")
        .isActive(true)
        .build();
  }

  private static VacancyDetail detail() {
    return new VacancyDetail(
        "1",
        "New title",
        "EPAM Systems",
        "Минск",
        "Беларусь",
        "2026-11-21",
        "FULLTIME",
        "new description",
        "new requirements",
        "new responsibilities",
        List.of("Java"));
  }
}
