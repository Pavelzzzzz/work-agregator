package com.vacancyscout.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vacancyscout.dto.SearchFilters;
import com.vacancyscout.dto.SearchResponse;
import com.vacancyscout.service.VacancySearchService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest(controllers = VacancyController.class)
public class VacancyControllerTest {
  @Autowired private WebTestClient webTestClient;

  @MockBean private VacancySearchService searchService;

  @Test
  void search_withoutResults_returnsEmptyResponse() {
    Mockito.when(searchService.search(any()))
        .thenReturn(Mono.just(new SearchResponse<>(0L, List.of(), 0, 20)));
    webTestClient
        .get()
        .uri("/api/vacancies/search")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.total")
        .isEqualTo(0);
  }

  @Test
  void search_defaultParams_returnsOk() {
    when(searchService.search(any()))
        .thenReturn(Mono.just(new SearchResponse<>(0L, List.of(), 0, 20)));

    webTestClient.get().uri("/api/vacancies/search").exchange().expectStatus().isOk();
  }

  @Test
  void search_withQuery_passesCorrectFilter() {
    when(searchService.search(any()))
        .thenReturn(Mono.just(new SearchResponse<>(0L, List.of(), 0, 20)));

    webTestClient.get().uri("/api/vacancies/search?q=Java").exchange().expectStatus().isOk();

    ArgumentCaptor<SearchFilters> captor = ArgumentCaptor.forClass(SearchFilters.class);
    verify(searchService).search(captor.capture());
    assertEquals("Java", captor.getValue().query());
  }

  @Test
  void search_withAllFilters_passesCorrectly() {
    when(searchService.search(any()))
        .thenReturn(Mono.just(new SearchResponse<>(0L, List.of(), 0, 10)));

    webTestClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("/api/vacancies/search")
                    .queryParam("q", "developer")
                    .queryParam("language", "en")
                    .queryParam("source", "rabota.by")
                    .queryParam("employmentType", "FULL_TIME")
                    .queryParam("minSalary", "1000")
                    .queryParam("maxSalary", "5000")
                    .queryParam("skills", "Java", "Spring")
                    .queryParam("companyName", "Google")
                    .queryParam("location", "Minsk")
                    .queryParam("remoteOnly", "true")
                    .queryParam("page", "2")
                    .queryParam("pageSize", "10")
                    .build())
        .exchange()
        .expectStatus()
        .isOk();

    ArgumentCaptor<SearchFilters> captor = ArgumentCaptor.forClass(SearchFilters.class);
    verify(searchService).search(captor.capture());
    SearchFilters f = captor.getValue();
    assertEquals("developer", f.query());
    assertEquals("en", f.language());
    assertEquals("rabota.by", f.source());
    assertEquals("FULL_TIME", f.employmentType());
    assertEquals(new BigDecimal("1000"), f.minSalary());
    assertEquals(new BigDecimal("5000"), f.maxSalary());
    assertEquals(List.of("Java", "Spring"), f.skills());
    assertEquals("Google", f.companyName());
    assertEquals("Minsk", f.location());
    assertEquals(true, f.remoteOnly());
    assertEquals(2, f.page());
    assertEquals(10, f.pageSize());
  }

  @Test
  void search_withSkills_passesAsList() {
    when(searchService.search(any()))
        .thenReturn(Mono.just(new SearchResponse<>(0L, List.of(), 0, 20)));

    webTestClient
        .get()
        .uri("/api/vacancies/search?skills=Java&skills=Spring&skills=Docker")
        .exchange()
        .expectStatus()
        .isOk();

    ArgumentCaptor<SearchFilters> captor = ArgumentCaptor.forClass(SearchFilters.class);
    verify(searchService).search(captor.capture());
    List<String> skills = captor.getValue().skills();
    assertEquals(3, skills.size());
    assertEquals(List.of("Java", "Spring", "Docker"), skills);
  }

  @Test
  void search_withPagination_passesCorrectPage() {
    when(searchService.search(any()))
        .thenReturn(Mono.just(new SearchResponse<>(0L, List.of(), 3, 5)));

    webTestClient
        .get()
        .uri("/api/vacancies/search?page=3&pageSize=5")
        .exchange()
        .expectStatus()
        .isOk();

    ArgumentCaptor<SearchFilters> captor = ArgumentCaptor.forClass(SearchFilters.class);
    verify(searchService).search(captor.capture());
    assertEquals(3, captor.getValue().page());
    assertEquals(5, captor.getValue().pageSize());
  }

  @Test
  void search_whenServiceThrows_returnsError() {
    when(searchService.search(any())).thenReturn(Mono.error(new RuntimeException("db error")));

    webTestClient.get().uri("/api/vacancies/search").exchange().expectStatus().is5xxServerError();
  }
}
