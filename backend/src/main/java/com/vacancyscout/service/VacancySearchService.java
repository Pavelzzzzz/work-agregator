package com.vacancyscout.service;

import com.vacancyscout.dto.SearchFilters;
import com.vacancyscout.dto.SearchResponse;
import com.vacancyscout.model.Vacancy;
import com.vacancyscout.repository.VacancyRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@Service
public class VacancySearchService {

    private final VacancyRepository vacancyRepository;

    public VacancySearchService(VacancyRepository vacancyRepository) {
        this.vacancyRepository = vacancyRepository;
    }

    public Mono<SearchResponse<Vacancy>> search(SearchFilters filters) {
        int page = (filters.page() != null) ? filters.page() : 0;
        int pageSize = (filters.pageSize() != null) ? filters.pageSize() : 20;

        Flux<Vacancy> results;
        if (filters.query() != null && !filters.query().isBlank()) {
            String lang = (filters.language() != null) ? filters.language() : "ru";
            if ("en".equalsIgnoreCase(lang)) {
                results = vacancyRepository.searchInEnglish(filters.query(), 1000, 0);
            } else {
                results = vacancyRepository.searchInRussian(filters.query(), 1000, 0);
            }
        } else {
            results = vacancyRepository.findAllByIsActiveTrueOrderByPostedAtDesc();
        }

        Flux<Vacancy> filtered = results;

        return Mono.zip(
            filtered.count(),
            filtered.skip((long) page * pageSize).take(pageSize).collectList()
        ).map(tuple -> new SearchResponse<Vacancy>(tuple.getT1(), tuple.getT2(), page, pageSize));
    }
}
