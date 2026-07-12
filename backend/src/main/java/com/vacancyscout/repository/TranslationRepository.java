package com.vacancyscout.repository;

import com.vacancyscout.model.VacancyTranslation;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface TranslationRepository extends R2dbcRepository<VacancyTranslation, UUID> {

  Flux<VacancyTranslation> findByLang(String lang);
}
