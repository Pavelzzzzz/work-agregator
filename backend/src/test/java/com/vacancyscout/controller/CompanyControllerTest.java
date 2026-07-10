package com.vacancyscout.controller;

import com.vacancyscout.service.company.CompanyService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

@WebFluxTest(controllers = CompanyController.class)
public class CompanyControllerTest {
  @Autowired private WebTestClient webTestClient;

  @MockBean private CompanyService companyService;

  @Test
  void list_shouldReturnOk() {
    Mockito.when(companyService.listAll()).thenReturn(Flux.empty());
    webTestClient.get().uri("/api/companies").exchange().expectStatus().isOk();
  }
}
