package com.vacancyscout.scheduler;

import com.vacancyscout.model.Company;
import com.vacancyscout.repository.CompanyRepository;
import com.vacancyscout.service.company.CompanyScanService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Periodically scans companies that have not been checked yet. */
@Component
@ConditionalOnProperty(
    prefix = "company.scan",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class CompanyScanScheduler {

  private static final Logger LOG = LoggerFactory.getLogger(CompanyScanScheduler.class);
  private static final List<String> RETRY_STATUSES =
      List.of(
          CompanyScanService.STATUS_PENDING,
          CompanyScanService.STATUS_ERROR,
          CompanyScanService.STATUS_NO_URL);

  private final CompanyRepository companyRepository;
  private final CompanyScanService scanService;
  private final int batchSize;
  private final int concurrency;

  public CompanyScanScheduler(
      CompanyRepository companyRepository,
      CompanyScanService scanService,
      @Value("${company.scan.batch-size:20}") int batchSize,
      @Value("${company.scan.concurrency:4}") int concurrency) {
    this.companyRepository = companyRepository;
    this.scanService = scanService;
    this.batchSize = batchSize;
    this.concurrency = concurrency;
  }

  @Scheduled(
      fixedDelayString = "${company.scan.interval-ms:300000}",
      initialDelayString = "${company.scan.initial-delay-ms:10000}")
  public void scanCompanies() {
    Flux.fromIterable(RETRY_STATUSES)
        .flatMap(companyRepository::findByIsActiveTrueAndScanStatusOrderByCreatedAtAsc, 3)
        .take(batchSize)
        .flatMap(this::scanSafely, concurrency)
        .doOnComplete(() -> LOG.info("Company scan batch finished"))
        .subscribe();
  }

  private Mono<Void> scanSafely(Company company) {
    return scanService
        .scan(company)
        .onErrorResume(e -> {
          LOG.error("Unexpected error while scanning company {}", company.getId(), e);
          return Mono.empty();
        });
  }
}
