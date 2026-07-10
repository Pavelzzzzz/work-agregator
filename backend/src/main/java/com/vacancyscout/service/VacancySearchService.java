package com.vacancyscout.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vacancyscout.dto.SearchFilters;
import com.vacancyscout.dto.SearchResponse;
import com.vacancyscout.model.Vacancy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class VacancySearchService {

  private final DatabaseClient db;
  private final ObjectMapper objectMapper;

  public VacancySearchService(DatabaseClient db, ObjectMapper objectMapper) {
    this.db = db;
    this.objectMapper = objectMapper;
  }

  public Mono<SearchResponse<Vacancy>> search(SearchFilters filters) {
    int page = filters.page() != null ? filters.page() : 0;
    int pageSize = filters.pageSize() != null ? filters.pageSize() : 20;

    String lang = resolveLang(filters.language());

    var params = new ArrayList<>();
    var where = new StringJoiner(" AND ");
    where.add("v.is_active = true");

    if (hasText(filters.query())) {
      where.add("vt.search_vector @@ to_tsquery($" + params.size() + ")");
      params.add(filters.query());
    }

    if (hasText(filters.source())) {
      where.add("v.source_name = $" + params.size());
      params.add(filters.source());
    }

    if (hasText(filters.employmentType())) {
      where.add("v.employment_type = $" + params.size());
      params.add(filters.employmentType());
    }

    if (hasText(filters.companyName())) {
      where.add("v.company_name ILIKE '%' || $" + params.size() + " || '%'");
      params.add(filters.companyName());
    }

    if (hasText(filters.location())) {
      where.add("v.location ILIKE '%' || $" + params.size() + " || '%'");
      params.add(filters.location());
    }

    if (filters.remoteOnly() != null && filters.remoteOnly()) {
      where.add("v.location IS NULL");
    }

    if (filters.minSalary() != null) {
      where.add(
          "(v.salary_max >= $" + params.size() + " OR v.salary_min >= $" + params.size() + ")");
      params.add(filters.minSalary());
    }

    if (filters.maxSalary() != null) {
      where.add(
          "(v.salary_min <= $" + params.size() + " OR v.salary_max <= $" + params.size() + ")");
      params.add(filters.maxSalary());
    }

    if (filters.skills() != null && !filters.skills().isEmpty()) {
      where.add("v.skills ?| string_to_array($" + params.size() + ", ',')");
      params.add(String.join(",", filters.skills()));
    }

    String whereClause = where.toString();

    boolean hasQuery = hasText(filters.query());
    String joinClause =
        hasQuery
            ? " LEFT JOIN vacancy_translations vt ON vt.vacancy_id = v.id AND vt.lang = '"
                + lang
                + "'"
            : "";

    Map<String, Object> bindings = toMap(params);

    String countSql =
        "SELECT COUNT(*) AS cnt FROM vacancies v" + joinClause + " WHERE " + whereClause;

    String dataSql =
        "SELECT v.* FROM vacancies v"
            + joinClause
            + " WHERE "
            + whereClause
            + " ORDER BY v.posted_at DESC LIMIT "
            + pageSize
            + " OFFSET "
            + (page * pageSize);

    Mono<Long> countMono =
        db.sql(countSql).bindValues(bindings).map((row, meta) -> row.get("cnt", Long.class)).one();

    Mono<List<Vacancy>> dataMono =
        db.sql(dataSql)
            .bindValues(bindings)
            .map(
                (row, meta) -> {
                  List<String> skills = parseSkills(row.get("skills", String.class));
                  return Vacancy.builder()
                      .id(row.get("id", UUID.class))
                      .sourceId(row.get("source_id", String.class))
                      .sourceName(row.get("source_name", String.class))
                      .title(row.get("title", String.class))
                      .companyName(row.get("company_name", String.class))
                      .companyWebsite(row.get("company_website", String.class))
                      .description(row.get("description", String.class))
                      .requirements(row.get("requirements", String.class))
                      .responsibilities(row.get("responsibilities", String.class))
                      .salaryMin(row.get("salary_min", BigDecimal.class))
                      .salaryMax(row.get("salary_max", BigDecimal.class))
                      .salaryCurrency(row.get("salary_currency", String.class))
                      .location(row.get("location", String.class))
                      .employmentType(row.get("employment_type", String.class))
                      .experienceRequired(row.get("experience_required", String.class))
                      .skills(skills)
                      .postedAt(row.get("posted_at", LocalDateTime.class))
                      .url(row.get("url", String.class))
                      .isActive(row.get("is_active", Boolean.class))
                      .createdAt(row.get("created_at", LocalDateTime.class))
                      .updatedAt(row.get("updated_at", LocalDateTime.class))
                      .build();
                })
            .all()
            .collectList();

    return Mono.zip(countMono, dataMono)
        .map(tuple -> new SearchResponse<>(tuple.getT1(), tuple.getT2(), page, pageSize));
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

  private static Map<String, Object> toMap(List<Object> params) {
    var map = new LinkedHashMap<String, Object>();
    for (int i = 0; i < params.size(); i++) {
      map.put(String.valueOf(i), params.get(i));
    }
    return map;
  }

  private List<String> parseSkills(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      return objectMapper.readValue(json, new TypeReference<>() {});
    } catch (Exception e) {
      return List.of();
    }
  }
}
