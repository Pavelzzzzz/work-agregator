package com.vacancyscout.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vacancyscout.dto.SearchFilters;
import com.vacancyscout.dto.SearchResponse;
import com.vacancyscout.model.Vacancy;
import com.vacancyscout.repository.VacancyRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
public class VacancySearchServiceTest {

  private final VacancyRepository vacancyRepository = Mockito.mock(VacancyRepository.class);

  private final com.vacancyscout.service.VacancySearchService service =
      new VacancySearchService(vacancyRepository);

  @Test
  void search_withQuery_shouldReturnResults() {
    Vacancy v1 =
        Vacancy.builder()
            .id(UUID.randomUUID())
            .sourceId("hh")
            .sourceName("HH_RU")
            .title("Java Dev")
            .companyName("ACME")
            .skills(List.of("Java", "Spring"))
            .postedAt(LocalDateTime.now())
            .url("")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    Mockito.when(vacancyRepository.searchInRussian("Java", 1000, 0)).thenReturn(Flux.just(v1));

    SearchFilters filters =
        new SearchFilters("Java", "ru", null, null, null, null, null, null, null, false, 0, 20);
    Mono<SearchResponse<Vacancy>> result = service.search(filters);

    var resp = result.block();
    assertThat(resp).isNotNull();
    assertThat(resp.total()).isGreaterThanOrEqualTo(1);
    assertThat(resp.results()).isNotNull();
  }

  @Test
  void search_withoutQuery_shouldReturnEmpty() {
    Mockito.when(vacancyRepository.findAllByIsActiveTrueOrderByPostedAtDesc())
        .thenReturn(Flux.empty());
    SearchFilters filters =
        new SearchFilters(null, "ru", null, null, null, null, null, null, null, false, 0, 20);
    Mono<SearchResponse<Vacancy>> result = service.search(filters);
    SearchResponse<Vacancy> resp = result.block();
    assertThat(resp).isNotNull();
    assertThat(resp.total()).isZero();
    assertThat(resp.results()).isEmpty();
  }
}
