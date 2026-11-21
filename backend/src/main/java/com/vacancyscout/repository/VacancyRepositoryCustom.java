package com.vacancyscout.repository;

import com.vacancyscout.dto.SearchFilters;
import com.vacancyscout.model.Vacancy;
import java.time.LocalDateTime;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface VacancyRepositoryCustom {
  Flux<Vacancy> searchAll(SearchFilters filters, int page, int pageSize);

  Mono<Long> countAll(SearchFilters filters);

  Mono<Void> insertVacancy(Vacancy vacancy);

  /**
   * Oldest active vacancies with a source URL that have not been refreshed since {@code cutoff}.
   */
  Flux<Vacancy> findForRefresh(LocalDateTime cutoff, int limit);

  /** Replaces source-derived fields and marks the vacancy as refreshed. */
  Mono<Boolean> updateDetails(Vacancy vacancy);

  /** Records a refresh attempt that produced no usable data, so it is retried later. */
  Mono<Boolean> markChecked(UUID id);

  /** Hides a vacancy whose source page is gone (HTTP 404/410). */
  Mono<Boolean> deactivate(UUID id);
}
