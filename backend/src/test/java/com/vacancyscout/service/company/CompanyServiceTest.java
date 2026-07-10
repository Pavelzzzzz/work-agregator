package com.vacancyscout.service.company;

import static org.assertj.core.api.Assertions.assertThat;

import com.vacancyscout.model.Company;
import com.vacancyscout.repository.CompanyRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;

public class CompanyServiceTest {
  private final CompanyRepository companyRepository = Mockito.mock(CompanyRepository.class);
  private final CompanyService service = new CompanyService(companyRepository);

  @Test
  void listAll_shouldReturnActiveCompanies() {
    Company c = Company.builder().id(UUID.randomUUID()).name("ACME").isActive(true).build();
    Mockito.when(companyRepository.findAllByIsActiveTrue()).thenReturn(Flux.just(c));
    var result = service.listAll().collectList().block();
    assertThat(result).isNotNull();
    assertThat(result).hasSizeGreaterThan(0);
  }
}
