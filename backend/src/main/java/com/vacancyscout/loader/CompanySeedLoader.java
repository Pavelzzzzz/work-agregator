package com.vacancyscout.loader;

import com.vacancyscout.model.Company;
import com.vacancyscout.repository.CompanyRepository;
import java.time.LocalDateTime;
import java.util.List;
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
      List<Company> seeds =
          List.of(
              seed(
                  "EPAM Systems",
                  "https://career.epam.com",
                  "https://www.epam.com",
                  "Global IT services"),
              seed(
                  "IBA Group",
                  "https://careers.ibagroup.com",
                  "https://www.ibagroup.com",
                  "IT consulting and software engineering"),
              seed(
                  "Wargaming",
                  "https://www.wargaming.net/careers/",
                  "https://www.wargaming.net",
                  "Online gaming company"));
      companyRepository.saveAll(seeds).then().block();
    }
  }

  private static Company seed(
      String name, String careersUrl, String websiteUrl, String description) {
    return Company.builder()
        .id(UUID.randomUUID())
        .name(name)
        .careersUrl(careersUrl)
        .websiteUrl(websiteUrl)
        .description(description)
        .isActive(true)
        .scanStatus(STATUS_ACTIVE)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }
}
