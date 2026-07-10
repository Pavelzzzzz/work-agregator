package com.vacancyscout.controller;

import com.vacancyscout.dto.SearchFilters;
import com.vacancyscout.dto.SearchResponse;
import com.vacancyscout.service.VacancySearchService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import com.vacancyscout.model.Vacancy;
import java.util.UUID;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;

@WebFluxTest(controllers = VacancyController.class)
public class VacancyControllerTest {
    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private VacancySearchService searchService;

    @Test
    void search_shouldReturnEmptyWhenNoResults() {
        Mockito.when(searchService.search(any())).thenReturn(Mono.just(new SearchResponse<>(0L, List.of(), 0, 20)));
        webTestClient.get().uri("/api/vacancies/search").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.total").isEqualTo(0);
    }
}
