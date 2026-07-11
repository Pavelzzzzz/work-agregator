package com.vacancyscout.repository;

import com.vacancyscout.model.Vacancy;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface VacancyRepository extends R2dbcRepository<Vacancy, UUID>, VacancyRepositoryCustom {
  Flux<Vacancy> findBySourceName(String sourceName);

  Flux<Vacancy> findAllByIsActiveTrueOrderByPostedAtDesc();

  Mono<Vacancy> findBySourceNameAndSourceId(String sourceName, String sourceId);

  Mono<Boolean> existsBySourceNameAndSourceId(String sourceName, String sourceId);
}
