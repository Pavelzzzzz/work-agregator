package com.vacancyscout.service.vacancy;

import com.vacancyscout.ingestion.RabotaByVacancyDetailFetcher;
import com.vacancyscout.ingestion.RabotaByVacancyDetailFetcher.DetailFetchResult;
import com.vacancyscout.ingestion.RabotaByVacancyDetailFetcher.VacancyDetail;
import com.vacancyscout.model.Vacancy;
import com.vacancyscout.repository.VacancyRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

/** Re-reads an existing vacancy from its source so stored data does not go stale. */
@Service
public class VacancyRefreshService {

  public enum Outcome {
    UPDATED,
    GONE,
    FAILED
  }

  private static final Logger LOG = LoggerFactory.getLogger(VacancyRefreshService.class);

  private final VacancyRepository vacancyRepository;
  private final RabotaByVacancyDetailFetcher fetcher;
  private final Duration timeout;

  @Autowired
  public VacancyRefreshService(
      VacancyRepository vacancyRepository,
      RabotaByVacancyDetailFetcher fetcher,
      @Value("${vacancy.refresh.timeout-ms:10000}") long timeoutMs) {
    this(vacancyRepository, fetcher, Duration.ofMillis(timeoutMs));
  }

  VacancyRefreshService(
      VacancyRepository vacancyRepository, RabotaByVacancyDetailFetcher fetcher, Duration timeout) {
    this.vacancyRepository = vacancyRepository;
    this.fetcher = fetcher;
    this.timeout = timeout;
  }

  public Mono<Outcome> refresh(Vacancy vacancy) {
    if (!StringUtils.hasText(vacancy.url())) {
      return Mono.just(Outcome.FAILED);
    }

    return fetcher
        .fetchWithStatus(vacancy.url())
        .timeout(timeout)
        .onErrorResume(e -> {
          LOG.debug("Refresh attempt failed for {}: {}", vacancy.url(), e.toString());
          return Mono.just(new DetailFetchResult(null, false));
        })
        .flatMap(result -> {
          if (result.gone()) {
            LOG.info("Vacancy is gone at source, deactivating: {}", vacancy.url());
            return vacancyRepository.deactivate(vacancy.id()).thenReturn(Outcome.GONE);
          }
          VacancyDetail detail = result.detail();
          if (detail == null) {
            return vacancyRepository.markChecked(vacancy.id()).thenReturn(Outcome.FAILED);
          }
          return vacancyRepository
              .updateDetails(merge(vacancy, detail))
              .thenReturn(Outcome.UPDATED);
        })
        .doOnError(e -> LOG.error("Failed to refresh vacancy {}", vacancy.id(), e))
        .onErrorResume(e -> vacancyRepository.markChecked(vacancy.id()).thenReturn(Outcome.FAILED));
  }

  private static Vacancy merge(Vacancy v, VacancyDetail d) {
    return Vacancy.builder()
        .id(v.id())
        .sourceId(v.sourceId())
        .sourceName(v.sourceName())
        .title(text(d.title(), v.title()))
        .companyName(text(d.companyName(), v.companyName()))
        .companyWebsite(v.companyWebsite())
        .description(d.description() != null ? d.description() : "")
        .requirements(d.requirements() != null ? d.requirements() : "")
        .responsibilities(d.responsibilities() != null ? d.responsibilities() : "")
        .salaryMin(v.salaryMin())
        .salaryMax(v.salaryMax())
        .salaryCurrency(v.salaryCurrency())
        .location(text(d.locality(), v.location()))
        .employmentType(v.employmentType())
        .experienceRequired(v.experienceRequired())
        .skills(d.skills() != null ? d.skills() : List.of())
        .searchText(v.searchText())
        .postedAt(v.postedAt())
        .url(v.url())
        .isActive(true)
        .createdAt(v.createdAt())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  private static String text(String value, String fallback) {
    return StringUtils.hasText(value) ? value : fallback;
  }
}
