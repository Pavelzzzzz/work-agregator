package com.vacancyscout.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vacancyscout.dto.SearchFilters;
import com.vacancyscout.model.Vacancy;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class VacancyRepositoryImpl implements VacancyRepositoryCustom {

  private static final String INSERT_SQL =
      """
      INSERT INTO vacancies (id, source_id, source_name, title, company_name, company_website,
        description, requirements, responsibilities, salary_min, salary_max, salary_currency,
        location, employment_type, experience_required, skills, posted_at, url,
        is_active, created_at, updated_at)
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15,
        CAST($16 AS jsonb), $17, $18, $19, $20, $21)
      """;

  private final DatabaseClient db;
  private final ObjectMapper objectMapper;

  public VacancyRepositoryImpl(DatabaseClient db, ObjectMapper objectMapper) {
    this.db = db;
    this.objectMapper = objectMapper;
  }

  @Override
  public Flux<Vacancy> searchAll(SearchFilters filters, int page, int pageSize) {
    SqlBuilder sql = build(filters);
    String query =
        "SELECT v.* FROM vacancies v"
            + sql.join
            + " WHERE "
            + sql.where
            + " ORDER BY v.posted_at DESC LIMIT "
            + pageSize
            + " OFFSET "
            + (page * pageSize);
    return db.sql(query).bindValues(sql.params).mapProperties(Vacancy.class).all();
  }

  @Override
  public Mono<Long> countAll(SearchFilters filters) {
    SqlBuilder sql = build(filters);
    String query = "SELECT COUNT(*) AS cnt FROM vacancies v" + sql.join + " WHERE " + sql.where;
    return db.sql(query)
        .bindValues(sql.params)
        .map((row, meta) -> row.get("cnt", Long.class))
        .one();
  }

  @Override
  public Mono<Void> insertVacancy(Vacancy vacancy) {
    try {
      String skillsJson = objectMapper.writeValueAsString(vacancy.skills());
      var spec =
          db.sql(INSERT_SQL)
              .bind(0, vacancy.id())
              .bind(1, vacancy.sourceId())
              .bind(2, vacancy.sourceName())
              .bind(3, vacancy.title())
              .bind(4, vacancy.companyName());

      spec = bindValue(spec, 5, vacancy.companyWebsite());
      spec = bindValue(spec, 6, vacancy.description());
      spec = bindValue(spec, 7, vacancy.requirements());
      spec = bindValue(spec, 8, vacancy.responsibilities());

      spec = bindValue(spec, 9, vacancy.salaryMin());
      spec = bindValue(spec, 10, vacancy.salaryMax());
      spec = bindValue(spec, 11, vacancy.salaryCurrency());
      spec = bindValue(spec, 12, vacancy.location());
      spec = bindValue(spec, 13, vacancy.employmentType());
      spec = bindValue(spec, 14, vacancy.experienceRequired());

      spec = spec.bind(15, skillsJson);

      spec = bindValue(spec, 16, vacancy.postedAt());
      spec = bindValue(spec, 17, vacancy.url());
      spec =
          spec.bind(18, vacancy.isActive())
              .bind(19, vacancy.createdAt())
              .bind(20, vacancy.updatedAt());

      return spec.then();
    } catch (JsonProcessingException e) {
      return Mono.error(e);
    }
  }

  private DatabaseClient.GenericExecuteSpec bindValue(
      DatabaseClient.GenericExecuteSpec spec, int index, Object value) {
    if (value == null) {
      Class<?> type = inferType(index);
      return spec.bindNull(index, type);
    }
    return spec.bind(index, value);
  }

  private static Class<?> inferType(int index) {
    return switch (index) {
      case 5, 6, 7, 8, 11, 12, 13, 14, 17 -> String.class;
      case 9, 10 -> java.math.BigDecimal.class;
      case 16 -> java.time.LocalDateTime.class;
      default -> String.class;
    };
  }

  private static SqlBuilder build(SearchFilters filters) {
    var params = new LinkedHashMap<String, Object>();
    var where = new StringBuilder("v.is_active = true");
    String join = "";

    if (hasText(filters.query())) {
      String lang = resolveLang(filters.language());
      join = " LEFT JOIN vacancy_translations vt ON vt.vacancy_id = v.id AND vt.lang = :lang";
      params.put("lang", lang);
      where.append(" AND vt.search_vector @@ to_tsquery(:query)");
      params.put("query", filters.query());
    }

    if (hasText(filters.source())) {
      where.append(" AND v.source_name = :source");
      params.put("source", filters.source());
    }

    if (hasText(filters.employmentType())) {
      where.append(" AND v.employment_type = :employmentType");
      params.put("employmentType", filters.employmentType());
    }

    if (hasText(filters.companyName())) {
      where.append(" AND v.company_name ILIKE '%' || :companyName || '%'");
      params.put("companyName", filters.companyName());
    }

    if (hasText(filters.location())) {
      where.append(" AND v.location ILIKE '%' || :location || '%'");
      params.put("location", filters.location());
    }

    if (filters.remoteOnly() != null && filters.remoteOnly()) {
      where.append(" AND v.location IS NULL");
    }

    if (filters.minSalary() != null) {
      where.append(" AND (v.salary_max >= :minSalary OR v.salary_min >= :minSalary)");
      params.put("minSalary", filters.minSalary());
    }

    if (filters.maxSalary() != null) {
      where.append(" AND (v.salary_min <= :maxSalary OR v.salary_max <= :maxSalary)");
      params.put("maxSalary", filters.maxSalary());
    }

    if (filters.skills() != null && !filters.skills().isEmpty()) {
      where.append(" AND v.skills ?| string_to_array(:skills, ',')");
      params.put("skills", String.join(",", filters.skills()));
    }

    return new SqlBuilder(join, where.toString(), params);
  }

  private static String resolveLang(String lang) {
    if (lang == null) {
      return "russian";
    }
    if ("ru".equalsIgnoreCase(lang)) {
      return "russian";
    }
    if ("en".equalsIgnoreCase(lang)) {
      return "english";
    }
    return lang;
  }

  private static boolean hasText(String s) {
    return s != null && !s.isBlank();
  }

  private record SqlBuilder(String join, String where, Map<String, Object> params) {}
}
