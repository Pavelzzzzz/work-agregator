package com.vacancyscout.repository;

import com.vacancyscout.model.Company;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface CompanyRepository extends R2dbcRepository<Company, UUID> {
    Flux<Company> findAllByIsActiveTrue();
    Mono<Company> findByName(String name);
}
