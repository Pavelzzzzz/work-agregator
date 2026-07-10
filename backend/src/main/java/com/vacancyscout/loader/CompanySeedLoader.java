package com.vacancyscout.loader;

import com.vacancyscout.model.Company;
import com.vacancyscout.repository.CompanyRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

@Component
public class CompanySeedLoader {
    private final CompanyRepository companyRepository;

    public CompanySeedLoader(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        // Startup seed loader: insert a small set of initial companies if DB is empty
        if (companyRepository.count().block() == 0) {
            var seeds = List.of(
                new com.vacancyscout.model.Company(
                    UUID.randomUUID(),
                    "EPAM Systems",
                    "https://career.epam.com",
                    "https://www.epam.com",
                    "Global IT services",
                    true,
                    null,
                    "ACTIVE",
                    null,
                    LocalDateTime.now(),
                    LocalDateTime.now()
                ),
                new com.vacancyscout.model.Company(
                    UUID.randomUUID(),
                    "IBA Group",
                    "https://careers.ibagroup.com",
                    "https://www.ibagroup.com",
                    "IT consulting and software engineering",
                    true,
                    null,
                    "ACTIVE",
                    null,
                    LocalDateTime.now(),
                    LocalDateTime.now()
                ),
                new com.vacancyscout.model.Company(
                    UUID.randomUUID(),
                    "Wargaming",
                    "https://www.wargaming.net/careers/",
                    "https://www.wargaming.net",
                    "Online gaming company",
                    true,
                    null,
                    "ACTIVE",
                    null,
                    LocalDateTime.now(),
                    LocalDateTime.now()
                )
            );
            companyRepository.saveAll(seeds).subscribe();
        }
    }
}
