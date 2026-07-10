package com.vacancyscout.service.company;

import com.vacancyscout.model.Company;
import com.vacancyscout.repository.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CompanyServiceTest {
    private final CompanyRepository companyRepository = Mockito.mock(CompanyRepository.class);
    private final CompanyService service = new CompanyService(companyRepository);

    @Test
    void listAll_shouldReturnActiveCompanies() {
        Company c = new Company(UUID.randomUUID(), "ACME", null, null, null, true, null, null, null, null, null);
        Mockito.when(companyRepository.findAllByIsActiveTrue()).thenReturn(Flux.just(c));
        var result = service.listAll().collectList().block();
        assertThat(result).isNotNull();
        assertThat(result).hasSizeGreaterThan(0);
    }
}
