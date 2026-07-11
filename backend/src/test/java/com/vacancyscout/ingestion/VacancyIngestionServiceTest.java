package com.vacancyscout.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.vacancyscout.dto.EventType;
import com.vacancyscout.model.Company;
import com.vacancyscout.repository.CompanyRepository;
import com.vacancyscout.repository.VacancyRepository;
import com.vacancyscout.stream.VacancyUpdateStream;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class VacancyIngestionServiceTest {

  @Mock private RabotaByRssSource rssSource;
  @Mock private RabotaByVacancyDetailFetcher detailFetcher;
  @Mock private VacancyRepository vacancyRepository;
  @Mock private CompanyRepository companyRepository;
  @Mock private VacancyUpdateStream updateStream;

  private VacancyIngestionService service;

  @BeforeEach
  void setUp() {
    service =
        new VacancyIngestionService(
            rssSource, detailFetcher, vacancyRepository, companyRepository, updateStream,
            "java,python");
  }

  private RabotaByRssSource.RssItem sampleItem(String id, String title, String company) {
    return new RabotaByRssSource.RssItem(
        id, title, "https://rabota.by/vacancy/" + id, company, "Минск", "3000 BYN",
        LocalDateTime.of(2025, 7, 1, 10, 0));
  }

  private RabotaByVacancyDetailFetcher.VacancyDetail sampleDetail(String id, String title) {
    return new RabotaByVacancyDetailFetcher.VacancyDetail(
        id, title, "EPAM Systems", "Минск", "Беларусь", "2025-07-01", "FULLTIME",
        "<p>description</p>", "requirements", "responsibilities", List.of("Java", "Spring"));
  }

  @Test
  void scheduledIngestion_newItem_savesAndNotifies() {
    var item = sampleItem("111", "Java Dev", "EPAM");
    var detail = sampleDetail("111", "Java Dev");

    when(rssSource.fetch("java")).thenReturn(Flux.just(item));
    when(rssSource.fetch("python")).thenReturn(Flux.empty());
    when(vacancyRepository.existsBySourceNameAndSourceId("rabota.by", "111"))
        .thenReturn(Mono.just(false));
    when(detailFetcher.fetch("https://rabota.by/vacancy/111")).thenReturn(Mono.just(detail));
    when(companyRepository.save(any())).thenReturn(Mono.just(new Company()));
    when(vacancyRepository.insertVacancy(any())).thenReturn(Mono.empty());

    service.scheduledIngestion();

    verify(vacancyRepository, timeout(5000).times(1))
        .existsBySourceNameAndSourceId("rabota.by", "111");
    verify(vacancyRepository, timeout(5000).times(1)).insertVacancy(any());
    verify(updateStream, timeout(5000).times(1)).publish(any());
  }

  @Test
  void scheduledIngestion_existingItem_skipped() {
    var item = sampleItem("222", "Python Dev", "Wargaming");

    when(rssSource.fetch("java")).thenReturn(Flux.just(item));
    when(rssSource.fetch("python")).thenReturn(Flux.empty());
    when(vacancyRepository.existsBySourceNameAndSourceId("rabota.by", "222"))
        .thenReturn(Mono.just(true));

    service.scheduledIngestion();

    verify(vacancyRepository, timeout(5000).times(1))
        .existsBySourceNameAndSourceId("rabota.by", "222");
    verifyNoInteractions(detailFetcher);
    verify(vacancyRepository, timeout(5000).times(0)).insertVacancy(any());
  }

  @Test
  void scheduledIngestion_newCompany_createsCompany() {
    var item = sampleItem("333", "Go Dev", "NewCorp");
    var detail = sampleDetail("333", "Go Dev");

    when(rssSource.fetch("java")).thenReturn(Flux.just(item));
    when(rssSource.fetch("python")).thenReturn(Flux.empty());
    when(vacancyRepository.existsBySourceNameAndSourceId("rabota.by", "333"))
        .thenReturn(Mono.just(false));
    when(detailFetcher.fetch("https://rabota.by/vacancy/333")).thenReturn(Mono.just(detail));
    when(companyRepository.save(any())).thenReturn(Mono.just(new Company()));
    when(vacancyRepository.insertVacancy(any())).thenReturn(Mono.empty());

    service.scheduledIngestion();

    verify(companyRepository, timeout(5000).times(1)).save(any());
  }

  @Test
  void scheduledIngestion_existingCompany_continuesOnSaveError() {
    var item = sampleItem("444", "Java Dev", "ExistingCorp");
    var detail = sampleDetail("444", "Java Dev");

    when(rssSource.fetch("java")).thenReturn(Flux.just(item));
    when(rssSource.fetch("python")).thenReturn(Flux.empty());
    when(vacancyRepository.existsBySourceNameAndSourceId("rabota.by", "444"))
        .thenReturn(Mono.just(false));
    when(detailFetcher.fetch("https://rabota.by/vacancy/444")).thenReturn(Mono.just(detail));
    when(companyRepository.save(any())).thenReturn(Mono.error(new RuntimeException("duplicate")));
    when(vacancyRepository.insertVacancy(any())).thenReturn(Mono.empty());

    service.scheduledIngestion();

    verify(companyRepository, timeout(5000).times(1)).save(any());
    verify(vacancyRepository, timeout(5000).times(1)).insertVacancy(any());
  }

  @Test
  void scheduledIngestion_fetcherReturnsEmpty_usesFallbackDetail() {
    var item = sampleItem("555", "Rust Dev", "FallbackCorp");

    when(rssSource.fetch("java")).thenReturn(Flux.just(item));
    when(rssSource.fetch("python")).thenReturn(Flux.empty());
    when(vacancyRepository.existsBySourceNameAndSourceId("rabota.by", "555"))
        .thenReturn(Mono.just(false));
    when(detailFetcher.fetch("https://rabota.by/vacancy/555")).thenReturn(Mono.empty());
    when(companyRepository.save(any())).thenReturn(Mono.just(new Company()));
    when(vacancyRepository.insertVacancy(any())).thenReturn(Mono.empty());

    service.scheduledIngestion();

    verify(vacancyRepository, timeout(5000).times(1)).insertVacancy(any());
  }

  @Test
  void scheduledIngestion_nullCompanyName_doesNotCreateCompany() {
    var item =
        new RabotaByRssSource.RssItem(
            "666", "Mystery Dev", "https://rabota.by/vacancy/666", null, null, null, null);
    var detail =
        new RabotaByVacancyDetailFetcher.VacancyDetail(
            "666", "Mystery Dev", null, null, null, null, null, null, null, null, List.of());

    when(rssSource.fetch("java")).thenReturn(Flux.just(item));
    when(rssSource.fetch("python")).thenReturn(Flux.empty());
    when(vacancyRepository.existsBySourceNameAndSourceId("rabota.by", "666"))
        .thenReturn(Mono.just(false));
    when(detailFetcher.fetch("https://rabota.by/vacancy/666")).thenReturn(Mono.just(detail));
    when(vacancyRepository.insertVacancy(any())).thenReturn(Mono.empty());

    service.scheduledIngestion();

    verify(companyRepository, timeout(5000).times(0)).save(any());
  }

  @Test
  void scheduledIngestion_multipleItems_processesAll() {
    var item1 = sampleItem("100", "Java Dev", "CompanyA");
    var item2 = sampleItem("200", "Python Dev", "CompanyB");
    var detail1 = sampleDetail("100", "Java Dev");
    var detail2 = sampleDetail("200", "Python Dev");

    when(rssSource.fetch("java")).thenReturn(Flux.just(item1, item2));
    when(rssSource.fetch("python")).thenReturn(Flux.empty());
    when(vacancyRepository.existsBySourceNameAndSourceId("rabota.by", "100"))
        .thenReturn(Mono.just(false));
    when(vacancyRepository.existsBySourceNameAndSourceId("rabota.by", "200"))
        .thenReturn(Mono.just(false));
    when(detailFetcher.fetch("https://rabota.by/vacancy/100")).thenReturn(Mono.just(detail1));
    when(detailFetcher.fetch("https://rabota.by/vacancy/200")).thenReturn(Mono.just(detail2));
    when(companyRepository.save(any())).thenReturn(Mono.just(new Company()));
    when(vacancyRepository.insertVacancy(any())).thenReturn(Mono.empty());

    service.scheduledIngestion();

    verify(vacancyRepository, timeout(5000).times(2)).insertVacancy(any());
    verify(updateStream, timeout(5000).times(2)).publish(any());
  }

  @Test
  void scheduledIngestion_notifierSendsCorrectEventType() {
    var item = sampleItem("777", "K8s Dev", "CloudInc");
    var detail = sampleDetail("777", "K8s Dev");

    when(rssSource.fetch("java")).thenReturn(Flux.just(item));
    when(rssSource.fetch("python")).thenReturn(Flux.empty());
    when(vacancyRepository.existsBySourceNameAndSourceId("rabota.by", "777"))
        .thenReturn(Mono.just(false));
    when(detailFetcher.fetch("https://rabota.by/vacancy/777")).thenReturn(Mono.just(detail));
    when(companyRepository.save(any())).thenReturn(Mono.just(new Company()));
    when(vacancyRepository.insertVacancy(any())).thenReturn(Mono.empty());

    var eventCaptor = ArgumentCaptor.forClass(com.vacancyscout.dto.VacancyUpdateEvent.class);

    service.scheduledIngestion();

    verify(updateStream, timeout(5000).times(1)).publish(eventCaptor.capture());
    var event = eventCaptor.getValue();
    assertThat(event.eventType()).isEqualTo(EventType.NEW);
    assertThat(event.sourceName()).isEqualTo("rabota.by");
    assertThat(event.title()).isEqualTo("K8s Dev");
  }
}
