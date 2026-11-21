package com.vacancyscout.service.company;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vacancyscout.model.Company;
import com.vacancyscout.model.Vacancy;
import com.vacancyscout.repository.CompanyRepository;
import com.vacancyscout.repository.VacancyRepository;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class CompanyScanServiceTest {

  @Mock private CompanyRepository companyRepository;
  @Mock private VacancyRepository vacancyRepository;

  private AutoCloseable mocks;

  @BeforeEach
  void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
    when(companyRepository.save(any(Company.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
  }

  @AfterEach
  void tearDown() throws Exception {
    mocks.close();
  }

  @Test
  void successfulCareersPageMarksCompanyActive() {
    Company company = company("EPAM Systems", "https://careers.example.com", null);

    serviceFor(HttpStatus.OK).scan(company).block();

    assertEquals(CompanyScanService.STATUS_ACTIVE, company.getScanStatus());
    assertNull(company.getScanError());
    assertNotNull(company.getLastScanAt());
    verify(companyRepository).save(company);
  }

  @Test
  void failingCareersPageMarksCompanyError() {
    Company company = company("EPAM Systems", "https://careers.example.com", null);

    serviceFor(HttpStatus.INTERNAL_SERVER_ERROR).scan(company).block();

    assertEquals(CompanyScanService.STATUS_ERROR, company.getScanStatus());
    assertTrue(company.getScanError().contains("HTTP 500"));
    assertNotNull(company.getLastScanAt());
  }

  @Test
  void fallsBackToSourceVacancyWhenCompanyHasNoUrls() {
    when(vacancyRepository.findFirstByCompanyNameAndIsActiveTrueOrderByPostedAtDesc("EPAM Systems"))
        .thenReturn(
            Mono.just(
                Vacancy.builder()
                    .id(UUID.randomUUID())
                    .companyName("EPAM Systems")
                    .url("https://rabota.by/vacancy/1")
                    .isActive(true)
                    .build()));
    Company company = company("EPAM Systems", null, null);

    serviceFor(HttpStatus.OK).scan(company).block();

    assertEquals(CompanyScanService.STATUS_ACTIVE, company.getScanStatus());
    verify(vacancyRepository)
        .findFirstByCompanyNameAndIsActiveTrueOrderByPostedAtDesc("EPAM Systems");
  }

  @Test
  void companyWithoutAnySourceIsMarkedNoUrl() {
    when(vacancyRepository.findFirstByCompanyNameAndIsActiveTrueOrderByPostedAtDesc("Unknown"))
        .thenReturn(Mono.empty());
    Company company = company("Unknown", null, null);

    serviceFor(HttpStatus.OK).scan(company).block();

    assertEquals(CompanyScanService.STATUS_NO_URL, company.getScanStatus());
    assertTrue(company.getScanError().contains("No careers URL"));
  }

  @Test
  void transportErrorIsRecordedAsError() {
    WebClient webClient =
        WebClient.builder()
            .exchangeFunction(request -> Mono.error(new IllegalStateException("timeout")))
            .build();
    Company company = company("EPAM Systems", "https://careers.example.com", null);

    new CompanyScanService(companyRepository, vacancyRepository, 2000, webClient)
        .scan(company)
        .block();

    assertEquals(CompanyScanService.STATUS_ERROR, company.getScanStatus());
    assertEquals("timeout", company.getScanError());
  }

  private static Company company(String name, String careersUrl, String websiteUrl) {
    return Company.builder()
        .id(UUID.randomUUID())
        .name(name)
        .careersUrl(careersUrl)
        .websiteUrl(websiteUrl)
        .isActive(true)
        .scanStatus(CompanyScanService.STATUS_PENDING)
        .build();
  }

  private CompanyScanService serviceFor(HttpStatus status) {
    WebClient webClient =
        WebClient.builder()
            .exchangeFunction(request ->
                Mono.just(
                    ClientResponse.create(status)
                        .header("Content-Type", "text/plain")
                        .body("response")
                        .build()))
            .build();
    return new CompanyScanService(companyRepository, vacancyRepository, 2000, webClient);
  }
}
