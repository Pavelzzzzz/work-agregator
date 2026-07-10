package com.vacancyscout.service;

import com.vacancyscout.dto.SearchFilters;
import com.vacancyscout.dto.SearchResponse;
import com.vacancyscout.model.Vacancy;
import com.vacancyscout.repository.VacancyRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class VacancySearchService {

  private static final int SEARCH_LIMIT = VacancyRepository.DEFAULT_SEARCH_LIMIT;

  private final VacancyRepository vacancyRepository;

  public VacancySearchService(VacancyRepository vacancyRepository) {
    this.vacancyRepository = vacancyRepository;
  }

  public Mono<SearchResponse<Vacancy>> search(SearchFilters filters) {
    int page = filters.page() != null ? filters.page() : 0;
    int pageSize = filters.pageSize() != null ? filters.pageSize() : 20;

    Flux<Vacancy> results = resolveResults(filters);

    return results
        .collectList()
        .map(
            all -> {
              int from = page * pageSize;
              int to = Math.min(from + pageSize, all.size());
              List<Vacancy> pageItems = from < all.size() ? all.subList(from, to) : List.of();
              return new SearchResponse<>(all.size(), pageItems, page, pageSize);
            });
  }

  private Flux<Vacancy> resolveResults(SearchFilters filters) {
    if (filters.query() != null && !filters.query().isBlank()) {
      String lang = filters.language() != null ? filters.language() : "ru";
      if ("en".equalsIgnoreCase(lang)) {
        return vacancyRepository.searchInEnglish(filters.query(), SEARCH_LIMIT, 0);
      }
      return vacancyRepository.searchInRussian(filters.query(), SEARCH_LIMIT, 0);
    }
    return vacancyRepository.findAllByIsActiveTrueOrderByPostedAtDesc();
  }
}
