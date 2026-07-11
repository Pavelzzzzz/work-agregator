package com.vacancyscout.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.vacancyscout.model.Company;
import com.vacancyscout.service.company.CompanyService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@WebFluxTest(controllers = CompanyController.class)
public class CompanyControllerTest {
  @Autowired private WebTestClient webTestClient;

  @MockBean private CompanyService companyService;

  @Test
  void list_shouldReturnOk() {
    Mockito.when(companyService.listAll()).thenReturn(Flux.empty());
    webTestClient.get().uri("/api/companies").exchange().expectStatus().isOk();
  }

  @Test
  void getById_found_returnsCompany() {
    UUID id = UUID.randomUUID();
    Company company =
        Company.builder()
            .id(id)
            .name("Acme Corp")
            .careersUrl("https://acme.com/careers")
            .websiteUrl("https://acme.com")
            .description("A great company")
            .isActive(true)
            .build();

    when(companyService.getById(id)).thenReturn(Mono.just(company));

    webTestClient
        .get()
        .uri("/api/companies/{id}", id)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.id")
        .isEqualTo(id.toString())
        .jsonPath("$.name")
        .isEqualTo("Acme Corp")
        .jsonPath("$.careersUrl")
        .isEqualTo("https://acme.com/careers")
        .jsonPath("$.websiteUrl")
        .isEqualTo("https://acme.com")
        .jsonPath("$.description")
        .isEqualTo("A great company")
        .jsonPath("$.active")
        .isEqualTo(true);
  }

  @Test
  void getById_notFound_returnsEmptyBody() {
    UUID id = UUID.randomUUID();
    when(companyService.getById(id)).thenReturn(Mono.empty());

    webTestClient
        .get()
        .uri("/api/companies/{id}", id)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .isEmpty();
  }

  @Test
  void create_validBody_returnsSavedCompany() {
    UUID id = UUID.randomUUID();
    Company saved =
        Company.builder()
            .id(id)
            .name("New Corp")
            .careersUrl("https://new.com/careers")
            .websiteUrl("https://new.com")
            .description("New company")
            .isActive(true)
            .build();

    when(companyService.create(any(Company.class))).thenReturn(Mono.just(saved));

    Company request =
        Company.builder()
            .name("New Corp")
            .careersUrl("https://new.com/careers")
            .websiteUrl("https://new.com")
            .description("New company")
            .isActive(true)
            .build();

    webTestClient
        .post()
        .uri("/api/companies")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.id")
        .isEqualTo(id.toString())
        .jsonPath("$.name")
        .isEqualTo("New Corp")
        .jsonPath("$.careersUrl")
        .isEqualTo("https://new.com/careers");
  }

  @Test
  void create_invalidBody_returns400() {
    webTestClient
        .post()
        .uri("/api/companies")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("invalid json")
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void update_found_returnsOk() {
    UUID id = UUID.randomUUID();
    Company updated =
        Company.builder()
            .id(id)
            .name("Updated Corp")
            .careersUrl("https://updated.com/careers")
            .websiteUrl("https://updated.com")
            .description("Updated description")
            .isActive(true)
            .build();

    when(companyService.update(eq(id), any(Company.class))).thenReturn(Mono.just(updated));

    Company request =
        Company.builder()
            .name("Updated Corp")
            .careersUrl("https://updated.com/careers")
            .websiteUrl("https://updated.com")
            .description("Updated description")
            .isActive(true)
            .build();

    webTestClient
        .put()
        .uri("/api/companies/{id}", id)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.id")
        .isEqualTo(id.toString())
        .jsonPath("$.name")
        .isEqualTo("Updated Corp");
  }

  @Test
  void update_notFound_returnsEmptyBody() {
    UUID id = UUID.randomUUID();
    when(companyService.update(eq(id), any(Company.class))).thenReturn(Mono.empty());

    Company request =
        Company.builder().name("Ghost Corp").websiteUrl("https://ghost.com").isActive(true).build();

    webTestClient
        .put()
        .uri("/api/companies/{id}", id)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .isEmpty();
  }

  @Test
  void delete_found_returnsOk() {
    UUID id = UUID.randomUUID();
    when(companyService.delete(id)).thenReturn(Mono.empty());

    webTestClient.delete().uri("/api/companies/{id}", id).exchange().expectStatus().isOk();
  }

  @Test
  void delete_notFound_returnsOk() {
    UUID id = UUID.randomUUID();
    when(companyService.delete(id)).thenReturn(Mono.empty());

    webTestClient.delete().uri("/api/companies/{id}", id).exchange().expectStatus().isOk();
  }
}
