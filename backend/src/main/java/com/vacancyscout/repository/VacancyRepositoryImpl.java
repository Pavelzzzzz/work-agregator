package com.vacancyscout.repository;

import com.vacancyscout.dto.SearchFilters;
import com.vacancyscout.model.Vacancy;
import com.vacancyscout.model.VacancyTranslation;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class VacancyRepositoryImpl implements VacancyRepositoryCustom {

  private final R2dbcEntityTemplate entityTemplate;

  public VacancyRepositoryImpl(R2dbcEntityTemplate entityTemplate) {
    this.entityTemplate = entityTemplate;
  }

  @Override
  public Flux<Vacancy> searchAll(SearchFilters filters, int page, int pageSize) {
    var criteria = buildCriteria(filters);
    var query =
        Query.query(criteria)
            .sort(Sort.by("postedAt").descending())
            .limit(pageSize)
            .offset((long) page * pageSize);
    return entityTemplate
        .select(Vacancy.class)
        .matching(query)
        .all()
        .filter(v -> matchesSkills(v, filters.skills()));
  }

  @Override
  public Mono<Long> countAll(SearchFilters filters) {
    var criteria = buildCriteria(filters);
    var query = Query.query(criteria);
    if (filters.skills() == null || filters.skills().isEmpty()) {
      return entityTemplate.select(Vacancy.class).matching(query).count();
    }
    return entityTemplate
        .select(Vacancy.class)
        .matching(query)
        .all()
        .filter(v -> matchesSkills(v, filters.skills()))
        .count();
  }

  @Override
  public Mono<Void> insertVacancy(Vacancy vacancy) {
    String searchText = buildSearchText(vacancy);
    Vacancy enriched =
        Vacancy.builder()
            .id(vacancy.id())
            .sourceId(vacancy.sourceId())
            .sourceName(vacancy.sourceName())
            .title(vacancy.title())
            .companyName(vacancy.companyName())
            .companyWebsite(vacancy.companyWebsite())
            .description(vacancy.description())
            .requirements(vacancy.requirements())
            .responsibilities(vacancy.responsibilities())
            .salaryMin(vacancy.salaryMin())
            .salaryMax(vacancy.salaryMax())
            .salaryCurrency(vacancy.salaryCurrency())
            .location(vacancy.location())
            .employmentType(vacancy.employmentType())
            .experienceRequired(vacancy.experienceRequired())
            .skills(vacancy.skills())
            .searchText(searchText)
            .postedAt(vacancy.postedAt())
            .url(vacancy.url())
            .isActive(vacancy.isActive())
            .createdAt(vacancy.createdAt())
            .updatedAt(vacancy.updatedAt())
            .build();
    return entityTemplate.insert(Vacancy.class).using(enriched).then(insertTranslationFor(vacancy));
  }

  private Mono<Void> insertTranslationFor(Vacancy vacancy) {
    String lang = langForSource(vacancy.sourceName());
    var translation =
        new VacancyTranslation(
            UUID.randomUUID(),
            vacancy.id(),
            lang,
            vacancy.title(),
            vacancy.companyName(),
            vacancy.description(),
            vacancy.requirements(),
            vacancy.responsibilities());
    return entityTemplate.insert(VacancyTranslation.class).using(translation).then();
  }

  private static String buildSearchText(Vacancy v) {
    return String.join(
        " ",
        nullToEmpty(v.title()),
        nullToEmpty(v.companyName()),
        nullToEmpty(v.description()),
        nullToEmpty(v.requirements()),
        nullToEmpty(v.responsibilities()));
  }

  private static String nullToEmpty(String s) {
    return s != null ? s : "";
  }

  private static boolean matchesSkills(Vacancy v, List<String> skills) {
    if (skills == null || skills.isEmpty()) {
      return true;
    }
    if (v.skills() == null || v.skills().isEmpty()) {
      return false;
    }
    for (String s : skills) {
      if (v.skills().contains(s)) {
        return true;
      }
    }
    return false;
  }

  private static Criteria buildCriteria(SearchFilters f) {
    var c = Criteria.where("isActive").is(true);

    if (isNotBlank(f.query())) {
      String textQuery = f.query().replaceAll("[&|!():*]", " ").replaceAll("\\s+", " ").trim();
      c = c.and(Criteria.where("searchText").like("%" + textQuery + "%").ignoreCase(true));
    }
    if (isNotBlank(f.source())) {
      c = c.and(Criteria.where("sourceName").is(f.source()));
    }
    if (isNotBlank(f.employmentType())) {
      c = c.and(Criteria.where("employmentType").is(f.employmentType()));
    }
    if (isNotBlank(f.companyName())) {
      c = c.and(Criteria.where("companyName").like("%" + f.companyName() + "%"));
    }
    if (isNotBlank(f.location())) {
      c = c.and(Criteria.where("location").like("%" + f.location() + "%"));
    }
    if (f.remoteOnly() != null && f.remoteOnly()) {
      c = c.and(Criteria.where("location").isNull());
    }
    if (f.minSalary() != null) {
      c =
          c.and(
              Criteria.where("salaryMax")
                  .greaterThanOrEquals(f.minSalary())
                  .or("salaryMin")
                  .greaterThanOrEquals(f.minSalary()));
    }
    if (f.maxSalary() != null) {
      c =
          c.and(
              Criteria.where("salaryMin")
                  .lessThanOrEquals(f.maxSalary())
                  .or("salaryMax")
                  .lessThanOrEquals(f.maxSalary()));
    }

    return c;
  }

  private static String langForSource(String source) {
    if ("RABOTA_BY".equals(source) || "HH_RU".equals(source)) {
      return "russian";
    }
    return "english";
  }

  private static boolean isNotBlank(String s) {
    return s != null && !s.isBlank();
  }
}
