package com.vacancyscout.controller;

import com.vacancyscout.dto.SearchFilters;
import com.vacancyscout.dto.SearchResponse;
import com.vacancyscout.model.Vacancy;
import com.vacancyscout.service.VacancySearchService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/vacancies")
public class VacancyController {

  private final VacancySearchService searchService;

  public VacancyController(VacancySearchService searchService) {
    this.searchService = searchService;
  }

  @GetMapping("/{id}")
  public Mono<Vacancy> get(@PathVariable UUID id) {
    return searchService.findById(id);
  }

  @GetMapping("/search")
  public Mono<SearchResponse<Vacancy>> search(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String language,
      @RequestParam(required = false) String source,
      @RequestParam(required = false) String employmentType,
      @RequestParam(required = false) BigDecimal minSalary,
      @RequestParam(required = false) BigDecimal maxSalary,
      @RequestParam(required = false) List<String> skills,
      @RequestParam(required = false) String companyName,
      @RequestParam(required = false) String location,
      @RequestParam(required = false) Boolean remoteOnly,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer pageSize) {
    SearchFilters filters =
        new SearchFilters(
            q,
            language,
            source,
            employmentType,
            minSalary,
            maxSalary,
            skills,
            companyName,
            location,
            remoteOnly,
            page,
            pageSize);
    return searchService.search(filters);
  }
}
