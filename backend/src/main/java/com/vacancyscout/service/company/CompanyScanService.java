package com.vacancyscout.service.company;

import com.vacancyscout.config.WebClientSupport;
import com.vacancyscout.model.Company;
import com.vacancyscout.model.Vacancy;
import com.vacancyscout.repository.CompanyRepository;
import com.vacancyscout.repository.VacancyRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/** Checks that a company's source is reachable and records the outcome. */
@Service
public class CompanyScanService {

  public static final String STATUS_PENDING = "PENDING";
  public static final String STATUS_ACTIVE = "ACTIVE";
  public static final String STATUS_ERROR = "ERROR";
  public static final String STATUS_NO_URL = "NO_URL";

  private static final Logger LOG = LoggerFactory.getLogger(CompanyScanService.class);
  private static final String NO_TARGET_MESSAGE = "No careers URL, website or source vacancy found";

  private final CompanyRepository companyRepository;
  private final VacancyRepository vacancyRepository;
  private final WebClient webClient;
  private final Duration timeout;

  @Autowired
  public CompanyScanService(
      CompanyRepository companyRepository,
      VacancyRepository vacancyRepository,
      @Value("${company.scan.timeout-ms:5000}") long timeoutMs) {
    this(
        companyRepository,
        vacancyRepository,
        timeoutMs,
        WebClient.builder()
            .exchangeStrategies(WebClientSupport.exchangeStrategies())
            .defaultHeader("User-Agent", "VacancyScout/1.0")
            .build());
  }

  CompanyScanService(
      CompanyRepository companyRepository,
      VacancyRepository vacancyRepository,
      long timeoutMs,
      WebClient webClient) {
    this.companyRepository = companyRepository;
    this.vacancyRepository = vacancyRepository;
    this.timeout = Duration.ofMillis(timeoutMs);
    this.webClient = webClient;
  }

  public Mono<Void> scan(Company company) {
    Mono<ScanResult> outcome =
        resolveTarget(company)
            .flatMap(this::probe)
            .switchIfEmpty(Mono.just(new ScanResult(STATUS_NO_URL, NO_TARGET_MESSAGE)))
            .onErrorResume(e -> Mono.just(new ScanResult(STATUS_ERROR, errorText(e))));

    return outcome.flatMap(result -> persist(company, result));
  }

  private Mono<String> resolveTarget(Company company) {
    if (StringUtils.hasText(company.getCareersUrl())) {
      return Mono.just(company.getCareersUrl().trim());
    }
    if (StringUtils.hasText(company.getWebsiteUrl())) {
      return Mono.just(company.getWebsiteUrl().trim());
    }
    if (!StringUtils.hasText(company.getName())) {
      return Mono.empty();
    }
    return vacancyRepository
        .findFirstByCompanyNameAndIsActiveTrueOrderByPostedAtDesc(company.getName())
        .map(Vacancy::url)
        .filter(StringUtils::hasText)
        .map(String::trim);
  }

  private Mono<ScanResult> probe(String url) {
    try {
      return webClient
          .get()
          .uri(url)
          .exchangeToMono(response -> {
            ScanResult result =
                response.statusCode().is2xxSuccessful()
                    ? new ScanResult(STATUS_ACTIVE, null)
                    : new ScanResult(STATUS_ERROR, "HTTP " + response.statusCode().value());
            return response.releaseBody().thenReturn(result);
          })
          .timeout(timeout)
          .onErrorResume(e -> Mono.just(new ScanResult(STATUS_ERROR, errorText(e))));
    } catch (RuntimeException e) {
      return Mono.just(new ScanResult(STATUS_ERROR, errorText(e)));
    }
  }

  private Mono<Void> persist(Company company, ScanResult result) {
    var now = LocalDateTime.now();
    company.setScanStatus(result.status());
    company.setScanError(result.error());
    company.setLastScanAt(now);
    company.setUpdatedAt(now);
    return companyRepository
        .save(company)
        .then()
        .doOnError(
            e -> LOG.error("Failed to persist scan result for company {}", company.getId(), e));
  }

  private static String errorText(Throwable error) {
    String message = error.getMessage();
    return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
  }

  private record ScanResult(String status, String error) {}
}
