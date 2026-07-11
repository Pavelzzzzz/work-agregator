package com.vacancyscout.service;

import com.vacancyscout.dto.SearchFilters;
import com.vacancyscout.dto.SearchResponse;
import com.vacancyscout.model.Vacancy;
import com.vacancyscout.repository.VacancyRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class VacancySearchService {

  private final VacancyRepository vacancyRepository;

  public VacancySearchService(VacancyRepository vacancyRepository) {
    this.vacancyRepository = vacancyRepository;
  }

  public Mono<SearchResponse<Vacancy>> search(SearchFilters filters) {
    int page = filters.page() != null ? filters.page() : 0;
    int pageSize = filters.pageSize() != null ? filters.pageSize() : 20;

    Mono<Long> count = vacancyRepository.countAll(filters);
    var results = vacancyRepository.searchAll(filters, page, pageSize).collectList();

    return Mono.zip(count, results)
        .map(tuple -> new SearchResponse<>(tuple.getT1(), tuple.getT2(), page, pageSize));
  }
}
