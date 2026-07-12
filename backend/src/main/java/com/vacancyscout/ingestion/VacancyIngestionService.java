package com.vacancyscout.ingestion;

import com.vacancyscout.dto.EventType;
import com.vacancyscout.dto.VacancyUpdateEvent;
import com.vacancyscout.model.Company;
import com.vacancyscout.model.Vacancy;
import com.vacancyscout.repository.CompanyRepository;
import com.vacancyscout.repository.VacancyRepository;
import com.vacancyscout.stream.VacancyUpdateStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Profile("!test")
public class VacancyIngestionService {

  private static final Logger LOG = LoggerFactory.getLogger(VacancyIngestionService.class);
  private static final String SOURCE_NAME = "rabota.by";
  private static final Pattern SALARY_RANGE =
      Pattern.compile(
          "(\\d+(?:[.,]\\d+)?)\\s*(?:-|–|до)\\s*(\\d+(?:[.,]\\d+)?)?\\s*(BYN|USD|EUR|RUB|б\\.р\\.)?",
          Pattern.CASE_INSENSITIVE);
  private static final Pattern SALARY_SINGLE =
      Pattern.compile(
          "(\\d+(?:[.,]\\d+)?)\\s*(BYN|USD|EUR|RUB|б\\.р\\.)?", Pattern.CASE_INSENSITIVE);
  private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

  private final RabotaByRssSource rssSource;
  private final RabotaByVacancyDetailFetcher detailFetcher;
  private final VacancyRepository vacancyRepository;
  private final CompanyRepository companyRepository;
  private final VacancyUpdateStream updateStream;
  private final String[] searchQueries;

  public VacancyIngestionService(
      RabotaByRssSource rssSource,
      RabotaByVacancyDetailFetcher detailFetcher,
      VacancyRepository vacancyRepository,
      CompanyRepository companyRepository,
      VacancyUpdateStream updateStream,
      @Value("${rabota.rss.queries}") String queriesConfig) {
    this.rssSource = rssSource;
    this.detailFetcher = detailFetcher;
    this.vacancyRepository = vacancyRepository;
    this.companyRepository = companyRepository;
    this.updateStream = updateStream;
    this.searchQueries = queriesConfig.split(",");
  }

  @Scheduled(fixedDelayString = "${rabota.rss.fetch-interval-ms}")
  public void scheduledIngestion() {
    LOG.info("Starting scheduled vacancy ingestion for {} queries", searchQueries.length);
    Flux.fromArray(searchQueries)
        .flatMap(query -> rssSource.fetch(query.trim()), 3)
        .filter(item -> item.vacancyId() != null)
        .flatMap(this::processItem)
        .doOnComplete(() -> LOG.info("Scheduled ingestion completed"))
        .subscribe();
  }

  private Mono<Void> processItem(RabotaByRssSource.RssItem item) {
    return vacancyRepository
        .existsBySourceNameAndSourceId(SOURCE_NAME, item.vacancyId())
        .flatMap(
            exists -> {
              if (exists) {
                return Mono.empty();
              }
              return fetchAndSave(item);
            });
  }

  private Mono<Void> fetchAndSave(RabotaByRssSource.RssItem item) {
    return detailFetcher
        .fetch(item.link())
        .defaultIfEmpty(
            new RabotaByVacancyDetailFetcher.VacancyDetail(
                item.vacancyId(),
                item.title(),
                item.companyName(),
                item.region(),
                "Беларусь",
                item.postedAt() != null ? item.postedAt().toString() : null,
                null,
                null,
                null,
                null,
                List.of()))
        .flatMap(
            detail -> {
              var vacancy = mapToVacancy(item, detail);
              return ensureCompany(vacancy.companyName())
                  .then(vacancyRepository.insertVacancy(vacancy))
                  .then(notify(vacancy));
            })
        .doOnError(e -> LOG.error("Failed to save vacancy: {}", item.link(), e))
        .onErrorResume(e -> Mono.empty());
  }

  private Mono<Void> ensureCompany(String companyName) {
    if (companyName == null) {
      return Mono.empty();
    }
    var now = LocalDateTime.now();
    var company =
        Company.builder()
            .id(UUID.randomUUID())
            .name(companyName)
            .isActive(true)
            .createdAt(now)
            .updatedAt(now)
            .build();
    return companyRepository
        .save(company)
        .then()
        .onErrorResume(
            e -> {
              // Unique constraint violation — company was just created by another concurrent
              // request
              return Mono.empty();
            });
  }

  private Vacancy mapToVacancy(
      RabotaByRssSource.RssItem item, RabotaByVacancyDetailFetcher.VacancyDetail detail) {
    var now = LocalDateTime.now();
    var salary = parseSalary(item.salaryRaw());
    var posted = parseDate(detail.datePosted() != null ? detail.datePosted() : item.postedAt());
    var empType = mapEmploymentType(detail.employmentType());
    var skills = detail.skills() != null ? detail.skills() : List.<String>of();

    return Vacancy.builder()
        .id(UUID.randomUUID())
        .sourceId(item.vacancyId())
        .sourceName(SOURCE_NAME)
        .title(detail.title() != null ? detail.title() : item.title())
        .companyName(detail.companyName() != null ? detail.companyName() : item.companyName())
        .companyWebsite(null)
        .description(detail.description() != null ? detail.description() : "")
        .requirements(detail.requirements() != null ? detail.requirements() : "")
        .responsibilities(detail.responsibilities() != null ? detail.responsibilities() : "")
        .salaryMin(salary != null ? salary.min() : null)
        .salaryMax(salary != null ? salary.max() : null)
        .salaryCurrency(salary != null ? salary.currency() : "BYN")
        .location(
            detail.locality() != null
                ? detail.locality()
                : (item.region() != null ? item.region() : ""))
        .employmentType(empType)
        .experienceRequired(null)
        .skills(skills)
        .postedAt(posted)
        .url(item.link())
        .isActive(true)
        .createdAt(now)
        .updatedAt(now)
        .build();
  }

  private SalaryInfo parseSalary(String raw) {
    if (!StringUtils.hasText(raw) || "не указан".equalsIgnoreCase(raw.trim())) {
      return null;
    }
    var m = SALARY_RANGE.matcher(raw);
    if (m.find()) {
      var min = parseDecimal(m.group(1));
      var max = parseDecimal(m.group(2));
      var cur = m.group(3);
      return new SalaryInfo(min, max, normalizeCurrency(cur));
    }
    m = SALARY_SINGLE.matcher(raw);
    if (m.find()) {
      var val = parseDecimal(m.group(1));
      var cur = m.group(2);
      return new SalaryInfo(val, null, normalizeCurrency(cur));
    }
    return null;
  }

  private BigDecimal parseDecimal(String s) {
    if (s == null) {
      return null;
    }
    try {
      return new BigDecimal(s.replace(",", "."));
    } catch (Exception e) {
      return null;
    }
  }

  private String normalizeCurrency(String cur) {
    if (cur == null) {
      return "BYN";
    }
    return switch (cur.toUpperCase(Locale.ROOT)) {
      case "RUB" -> "RUB";
      case "USD" -> "USD";
      case "EUR" -> "EUR";
      case "Б.Р." -> "BYN";
      default -> "BYN";
    };
  }

  private LocalDateTime parseDate(Object date) {
    if (date == null) {
      return LocalDateTime.now();
    }
    if (date instanceof LocalDateTime dt) {
      return dt;
    }
    var s = date.toString();
    try {
      return LocalDateTime.parse(s, ISO_FORMAT);
    } catch (Exception e) {
      return LocalDateTime.now();
    }
  }

  private String mapEmploymentType(String type) {
    if (type == null) {
      return null;
    }
    return switch (type.toLowerCase(Locale.ROOT)) {
      case "fulltime" -> "Полная занятость";
      case "parttime" -> "Частичная занятость";
      case "contractor" -> "Проектная работа";
      case "internship" -> "Стажировка";
      default -> type;
    };
  }

  private Mono<Void> notify(Vacancy vacancy) {
    var event =
        new VacancyUpdateEvent(
            UUID.randomUUID(),
            vacancy.id(),
            vacancy.title(),
            vacancy.companyName(),
            vacancy.postedAt(),
            SOURCE_NAME,
            EventType.NEW,
            vacancy.url());
    updateStream.publish(event);
    return Mono.empty();
  }

  private record SalaryInfo(BigDecimal min, BigDecimal max, String currency) {}
}
