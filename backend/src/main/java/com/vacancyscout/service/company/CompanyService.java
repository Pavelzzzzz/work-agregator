package com.vacancyscout.service.company;

import com.vacancyscout.model.Company;
import com.vacancyscout.repository.CompanyRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class CompanyService {
    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    public Flux<Company> listAll() {
        return companyRepository.findAllByIsActiveTrue();
    }

    public Mono<Company> getById(UUID id) {
        return companyRepository.findById(id);
    }

    public Mono<Company> create(Company company) {
        return companyRepository.save(company);
    }

    public Mono<Company> update(UUID id, Company company) {
        // Simple update by replacing fields; ensure id matches
        Company updated = new com.vacancyscout.model.Company(
            id,
            company.name(),
            company.careersUrl(),
            company.websiteUrl(),
            company.description(),
            company.isActive(),
            company.lastScanAt(),
            company.scanStatus(),
            company.scanError(),
            company.createdAt(),
            company.updatedAt()
        );
        return companyRepository.save(updated);
    }

    public Mono<Void> delete(UUID id) {
        return companyRepository.deleteById(id);
    }
}
