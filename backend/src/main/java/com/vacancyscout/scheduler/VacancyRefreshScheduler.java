package com.vacancyscout.scheduler;

import com.vacancyscout.model.Vacancy;
import com.vacancyscout.repository.VacancyRepository;
import com.vacancyscout.service.vacancy.VacancyRefreshService;
import com.vacancyscout.service.vacancy.VacancyRefreshService.Outcome;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/** Periodically re-reads stale vacancies so their data stays current. */
@Component
@ConditionalOnProperty(
    prefix = "vacancy.refresh",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class VacancyRefreshScheduler {

  private static final Logger LOG = LoggerFactory.getLogger(VacancyRefreshScheduler.class);

  private final VacancyRepository vacancyRepository;
  private final VacancyRefreshService refreshService;
  private final int batchSize;
  private final int concurrency;
  private final Duration staleAfter;

  public VacancyRefreshScheduler(
      VacancyRepository vacancyRepository,
      VacancyRefreshService refreshService,
      @Value("${vacancy.refresh.batch-size:50}") int batchSize,
      @Value("${vacancy.refresh.concurrency:4}") int concurrency,
      @Value("${vacancy.refresh.stale-after-ms:3600000}") long staleAfterMs) {
    this.vacancyRepository = vacancyRepository;
    this.refreshService = refreshService;
    this.batchSize = batchSize;
    this.concurrency = concurrency;
    this.staleAfter = Duration.ofMillis(staleAfterMs);
  }

  @Scheduled(
      fixedDelayString = "${vacancy.refresh.interval-ms:300000}",
      initialDelayString = "${vacancy.refresh.initial-delay-ms:30000}")
  public void refreshStaleVacancies() {
    var cutoff = LocalDateTime.now().minus(staleAfter);
    var updated = new AtomicInteger();
    var gone = new AtomicInteger();
    var failed = new AtomicInteger();

    vacancyRepository
        .findForRefresh(cutoff, batchSize)
        .flatMap(vacancy -> refreshSafely(vacancy, updated, gone, failed), concurrency)
        .doOnTerminate(() ->
            LOG.info(
                "Vacancy refresh batch finished: updated={}, gone={}, failed={}",
                updated.get(),
                gone.get(),
                failed.get()))
        .subscribe();
  }

  private Mono<Void> refreshSafely(
      Vacancy vacancy, AtomicInteger updated, AtomicInteger gone, AtomicInteger failed) {
    return refreshService
        .refresh(vacancy)
        .doOnNext(outcome -> {
          if (outcome == Outcome.UPDATED) {
            updated.incrementAndGet();
          } else if (outcome == Outcome.GONE) {
            gone.incrementAndGet();
          } else {
            failed.incrementAndGet();
          }
        })
        .onErrorResume(e -> {
          LOG.error("Unexpected error while refreshing vacancy {}", vacancy.id(), e);
          failed.incrementAndGet();
          return Mono.empty();
        })
        .then();
  }
}
