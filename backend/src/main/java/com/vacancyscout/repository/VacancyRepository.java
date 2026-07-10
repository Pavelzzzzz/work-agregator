package com.vacancyscout.repository;

import com.vacancyscout.model.Vacancy;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface VacancyRepository extends R2dbcRepository<Vacancy, UUID> {
  int DEFAULT_SEARCH_LIMIT = 1000;

  Flux<Vacancy> findBySourceName(String sourceName);

  Flux<Vacancy> findAllByIsActiveTrueOrderByPostedAtDesc();

  Mono<Vacancy> findBySourceNameAndSourceId(String sourceName, String sourceId);

  Mono<Boolean> existsBySourceNameAndSourceId(String sourceName, String sourceId);

  @Query(
      """
        SELECT v.* FROM vacancies v
        WHERE v.is_active = true
        AND EXISTS (
            SELECT 1 FROM vacancy_translations vt
            WHERE vt.vacancy_id = v.id
            AND vt.search_vector @@ to_tsquery('russian', :query)
        )
        ORDER BY v.posted_at DESC LIMIT :limit OFFSET :offset
    """)
  Flux<Vacancy> searchInRussian(String query, int limit, int offset);

  @Query(
      """
        SELECT v.* FROM vacancies v
        WHERE v.is_active = true
        AND EXISTS (
            SELECT 1 FROM vacancy_translations vt
            WHERE vt.vacancy_id = v.id
            AND vt.search_vector @@ to_tsquery('english', :query)
        )
        ORDER BY v.posted_at DESC LIMIT :limit OFFSET :offset
    """)
  Flux<Vacancy> searchInEnglish(String query, int limit, int offset);
}
