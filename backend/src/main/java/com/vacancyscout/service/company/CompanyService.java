package com.vacancyscout.service.company;

import com.vacancyscout.model.Company;
import com.vacancyscout.repository.CompanyRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
    return companyRepository
        .findById(id)
        .switchIfEmpty(Mono.error(new IllegalArgumentException("Company not found: " + id)))
        .then(
            Mono.defer(
                () -> {
                  Company updated =
                      Company.builder()
                          .id(id)
                          .name(company.getName())
                          .careersUrl(company.getCareersUrl())
                          .websiteUrl(company.getWebsiteUrl())
                          .description(company.getDescription())
                          .isActive(company.isActive())
                          .lastScanAt(company.getLastScanAt())
                          .scanStatus(company.getScanStatus())
                          .scanError(company.getScanError())
                          .createdAt(company.getCreatedAt())
                          .updatedAt(company.getUpdatedAt())
                          .build();
                  return companyRepository.save(updated);
                }));
  }

  public Mono<Void> delete(UUID id) {
    return companyRepository
        .findById(id)
        .switchIfEmpty(Mono.error(new IllegalArgumentException("Company not found: " + id)))
        .then(companyRepository.deleteById(id));
  }
}
