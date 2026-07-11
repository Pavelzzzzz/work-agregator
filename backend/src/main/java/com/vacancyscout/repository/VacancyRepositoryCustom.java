package com.vacancyscout.repository;

import com.vacancyscout.dto.SearchFilters;
import com.vacancyscout.model.Vacancy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface VacancyRepositoryCustom {
  Flux<Vacancy> searchAll(SearchFilters filters, int page, int pageSize);

  Mono<Long> countAll(SearchFilters filters);

  Mono<Void> insertVacancy(Vacancy vacancy);
}
