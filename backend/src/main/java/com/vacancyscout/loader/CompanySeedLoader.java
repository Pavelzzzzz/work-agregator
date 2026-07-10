package com.vacancyscout.loader;

import com.vacancyscout.model.Company;
import com.vacancyscout.repository.CompanyRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class CompanySeedLoader {
  private static final String STATUS_ACTIVE = "ACTIVE";

  private final CompanyRepository companyRepository;

  public CompanySeedLoader(CompanyRepository companyRepository) {
    this.companyRepository = companyRepository;
  }

  @EventListener
  public void onApplicationReady(ApplicationReadyEvent event) {
    if (companyRepository.count().block() == 0) {
      var now = LocalDateTime.now();
      var demo =
          Company.builder()
              .id(UUID.randomUUID())
              .name("EPAM Systems")
              .careersUrl("https://career.epam.com")
              .websiteUrl("https://www.epam.com")
              .description("Global IT services")
              .isActive(true)
              .scanStatus(STATUS_ACTIVE)
              .createdAt(now)
              .updatedAt(now)
              .build();
      companyRepository.save(demo).then().block();
    }
  }
}
