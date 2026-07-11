package com.vacancyscout.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vacancyscout.AbstractIntegrationTest;
import com.vacancyscout.dto.SearchFilters;
import com.vacancyscout.dto.SearchResponse;
import com.vacancyscout.model.Vacancy;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.r2dbc.core.DatabaseClient;

@Tag("integration")
class VacancySearchServiceIntegrationTest extends AbstractIntegrationTest {

  @Autowired private VacancySearchService searchService;

  @Autowired private DatabaseClient db;

  @Autowired private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    db.sql("TRUNCATE TABLE vacancy_translations, vacancies CASCADE").then().block();
  }

  private UUID createVacancy(
      String title,
      String companyName,
      String sourceName,
      String location,
      String employmentType,
      BigDecimal salaryMin,
      BigDecimal salaryMax,
      List<String> skills,
      boolean isActive) {
    UUID id = UUID.randomUUID();
    String skillsJson = "[]";
    if (skills != null && !skills.isEmpty()) {
      try {
        skillsJson = objectMapper.writeValueAsString(skills);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    db.sql(
            "INSERT INTO vacancies "
                + "(id, source_id, source_name, title, company_name, "
                + "employment_type, salary_min, salary_max, skills, location, "
                + "is_active, posted_at, created_at, updated_at) "
                + "VALUES ($0, $1, $2, $3, $4, $5, $6, $7, $8::jsonb, $9, "
                + "$10, NOW(), NOW(), NOW())")
        .bind("0", id)
        .bind("1", "src-" + UUID.randomUUID())
        .bind("2", sourceName)
        .bind("3", title)
        .bind("4", companyName)
        .bind("5", employmentType)
        .bind("6", salaryMin)
        .bind("7", salaryMax)
        .bind("8", skillsJson)
        .bind("9", location)
        .bind("10", isActive)
        .then()
        .block();
    return id;
  }

  private void createTranslation(UUID vacancyId, String lang, String title, String description) {
    db.sql(
            "INSERT INTO vacancy_translations "
                + "(vacancy_id, lang, title, description, search_vector) "
                + "VALUES ($0, $1, $2, $3, "
                + "to_tsvector($1, COALESCE($2, '') || ' ' || COALESCE($3, '')))")
        .bind("0", vacancyId)
        .bind("1", lang)
        .bind("2", title)
        .bind("3", description)
        .then()
        .block();
  }

  @Test
  void search_withoutFilters_returnsAllActive() {
    createVacancy(
        "Dev 1",
        "Company A",
        "SOURCE_1",
        "Minsk",
        "FULL_TIME",
        BigDecimal.valueOf(1000),
        BigDecimal.valueOf(2000),
        List.of("Java"),
        true);
    createVacancy(
        "Dev 2",
        "Company B",
        "SOURCE_2",
        "Remote",
        "PART_TIME",
        BigDecimal.valueOf(1500),
        BigDecimal.valueOf(2500),
        List.of("Python"),
        true);
    createVacancy(
        "Dev 3",
        "Company C",
        "SOURCE_1",
        "Moscow",
        "CONTRACT",
        BigDecimal.valueOf(500),
        BigDecimal.valueOf(1000),
        List.of("Go"),
        false);

    SearchFilters filters =
        new SearchFilters(null, null, null, null, null, null, null, null, null, null, 0, 20);

    SearchResponse<Vacancy> response = searchService.search(filters).block();

    assertThat(response.total()).isEqualTo(2);
    assertThat(response.results()).hasSize(2);
    assertThat(response.results())
        .extracting(Vacancy::title)
        .containsExactlyInAnyOrder("Dev 1", "Dev 2");
  }

  @Test
  void search_byFullTextQuery_returnsMatching() {
    UUID id1 =
        createVacancy(
            "Java Developer",
            "Company A",
            "SOURCE_1",
            "Minsk",
            "FULL_TIME",
            null,
            null,
            null,
            true);
    createTranslation(
        id1,
        "english",
        "Java Developer",
        "We need a skilled Java developer with Spring Boot experience");

    UUID id2 =
        createVacancy(
            "Python Developer",
            "Company B",
            "SOURCE_1",
            "Minsk",
            "FULL_TIME",
            null,
            null,
            null,
            true);
    createTranslation(
        id2, "english", "Python Developer", "We need a Python developer with Django experience");

    SearchFilters filters =
        new SearchFilters(
            "java & developer", "en", null, null, null, null, null, null, null, null, 0, 20);

    SearchResponse<Vacancy> response = searchService.search(filters).block();

    assertThat(response.total()).isEqualTo(1);
    assertThat(response.results()).hasSize(1);
    assertThat(response.results().get(0).title()).isEqualTo("Java Developer");
  }

  @Test
  void search_bySource_filtersCorrectly() {
    createVacancy("Dev 1", "Company A", "RABOTA_BY", "Minsk", "FULL_TIME", null, null, null, true);
    createVacancy("Dev 2", "Company B", "HH_RU", "Moscow", "FULL_TIME", null, null, null, true);
    createVacancy("Dev 3", "Company C", "RABOTA_BY", "SPB", "PART_TIME", null, null, null, true);

    SearchFilters filters =
        new SearchFilters(null, null, "RABOTA_BY", null, null, null, null, null, null, null, 0, 20);

    SearchResponse<Vacancy> response = searchService.search(filters).block();

    assertThat(response.total()).isEqualTo(2);
    assertThat(response.results()).hasSize(2);
    assertThat(response.results())
        .extracting(Vacancy::sourceName)
        .allMatch(s -> s.equals("RABOTA_BY"));
  }

  @Test
  void search_byEmploymentType_filtersCorrectly() {
    createVacancy("Dev 1", "Company A", "SRC", "Minsk", "FULL_TIME", null, null, null, true);
    createVacancy("Dev 2", "Company B", "SRC", "Minsk", "PART_TIME", null, null, null, true);
    createVacancy("Dev 3", "Company C", "SRC", "Minsk", "FULL_TIME", null, null, null, true);

    SearchFilters filters =
        new SearchFilters(null, null, null, "PART_TIME", null, null, null, null, null, null, 0, 20);

    SearchResponse<Vacancy> response = searchService.search(filters).block();

    assertThat(response.total()).isEqualTo(1);
    assertThat(response.results()).hasSize(1);
    assertThat(response.results().get(0).employmentType()).isEqualTo("PART_TIME");
  }

  @Test
  void search_byCompanyName_filtersCorrectly() {
    createVacancy("Dev 1", "EPAM Systems", "SRC", "Minsk", "FULL_TIME", null, null, null, true);
    createVacancy("Dev 2", "Google", "SRC", "Minsk", "FULL_TIME", null, null, null, true);
    createVacancy(
        "Dev 3", "EPAM Technologies", "SRC", "Minsk", "FULL_TIME", null, null, null, true);

    SearchFilters filters =
        new SearchFilters(null, null, null, null, null, null, null, "EPAM", null, null, 0, 20);

    SearchResponse<Vacancy> response = searchService.search(filters).block();

    assertThat(response.total()).isEqualTo(2);
    assertThat(response.results()).hasSize(2);
    assertThat(response.results())
        .extracting(Vacancy::companyName)
        .allMatch(name -> name.contains("EPAM"));
  }

  @Test
  void search_byLocation_filtersCorrectly() {
    createVacancy("Dev 1", "Company A", "SRC", "Minsk", "FULL_TIME", null, null, null, true);
    createVacancy("Dev 2", "Company B", "SRC", "Moscow", "FULL_TIME", null, null, null, true);
    createVacancy("Dev 3", "Company C", "SRC", "Minsk City", "FULL_TIME", null, null, null, true);

    SearchFilters filters =
        new SearchFilters(null, null, null, null, null, null, null, null, "Minsk", null, 0, 20);

    SearchResponse<Vacancy> response = searchService.search(filters).block();

    assertThat(response.total()).isEqualTo(2);
    assertThat(response.results()).hasSize(2);
    assertThat(response.results())
        .extracting(Vacancy::location)
        .allMatch(loc -> loc.contains("Minsk"));
  }

  @Test
  void search_remoteOnly_excludesLocated() {
    createVacancy("Dev 1", "Company A", "SRC", "Minsk", "FULL_TIME", null, null, null, true);
    createVacancy("Dev 2", "Company B", "SRC", null, "FULL_TIME", null, null, null, true);
    createVacancy("Dev 3", "Company C", "SRC", "Remote OK", "FULL_TIME", null, null, null, true);

    SearchFilters filters =
        new SearchFilters(null, null, null, null, null, null, null, null, null, true, 0, 20);

    SearchResponse<Vacancy> response = searchService.search(filters).block();

    assertThat(response.total()).isEqualTo(1);
    assertThat(response.results()).hasSize(1);
    assertThat(response.results().get(0).location()).isNull();
  }

  @Test
  void search_byMinSalary_filtersCorrectly() {
    createVacancy(
        "Dev 1",
        "Company A",
        "SRC",
        null,
        "FULL_TIME",
        BigDecimal.valueOf(1000),
        BigDecimal.valueOf(2000),
        null,
        true);
    createVacancy(
        "Dev 2",
        "Company B",
        "SRC",
        null,
        "FULL_TIME",
        BigDecimal.valueOf(3000),
        BigDecimal.valueOf(4000),
        null,
        true);
    createVacancy(
        "Dev 3",
        "Company C",
        "SRC",
        null,
        "FULL_TIME",
        BigDecimal.valueOf(500),
        BigDecimal.valueOf(800),
        null,
        true);

    SearchFilters filters =
        new SearchFilters(
            null, null, null, null, BigDecimal.valueOf(1000), null, null, null, null, null, 0, 20);

    SearchResponse<Vacancy> response = searchService.search(filters).block();

    assertThat(response.total()).isEqualTo(2);
    assertThat(response.results()).hasSize(2);
    assertThat(response.results())
        .extracting(Vacancy::title)
        .containsExactlyInAnyOrder("Dev 1", "Dev 2");
  }

  @Test
  void search_byMaxSalary_filtersCorrectly() {
    createVacancy(
        "Dev 1",
        "Company A",
        "SRC",
        null,
        "FULL_TIME",
        BigDecimal.valueOf(1000),
        BigDecimal.valueOf(2000),
        null,
        true);
    createVacancy(
        "Dev 2",
        "Company B",
        "SRC",
        null,
        "FULL_TIME",
        BigDecimal.valueOf(3000),
        BigDecimal.valueOf(4000),
        null,
        true);
    createVacancy(
        "Dev 3",
        "Company C",
        "SRC",
        null,
        "FULL_TIME",
        BigDecimal.valueOf(500),
        BigDecimal.valueOf(800),
        null,
        true);

    SearchFilters filters =
        new SearchFilters(
            null, null, null, null, null, BigDecimal.valueOf(2000), null, null, null, null, 0, 20);

    SearchResponse<Vacancy> response = searchService.search(filters).block();

    assertThat(response.total()).isEqualTo(2);
    assertThat(response.results()).hasSize(2);
    assertThat(response.results())
        .extracting(Vacancy::title)
        .containsExactlyInAnyOrder("Dev 1", "Dev 3");
  }

  @Test
  void search_bySkills_filtersCorrectly() {
    createVacancy(
        "Dev 1",
        "Company A",
        "SRC",
        null,
        "FULL_TIME",
        null,
        null,
        List.of("Java", "Spring", "PostgreSQL"),
        true);
    createVacancy(
        "Dev 2",
        "Company B",
        "SRC",
        null,
        "FULL_TIME",
        null,
        null,
        List.of("Python", "Django"),
        true);
    createVacancy(
        "Dev 3", "Company C", "SRC", null, "FULL_TIME", null, null, List.of("Java", "React"), true);

    SearchFilters filters =
        new SearchFilters(
            null, null, null, null, null, null, List.of("Spring"), null, null, null, 0, 20);

    SearchResponse<Vacancy> response = searchService.search(filters).block();

    assertThat(response.total()).isEqualTo(1);
    assertThat(response.results()).hasSize(1);
    assertThat(response.results().get(0).title()).isEqualTo("Dev 1");
  }

  @Test
  void search_withCombinedFilters_returnsIntersection() {
    createVacancy(
        "Dev 1",
        "EPAM",
        "RABOTA_BY",
        "Minsk",
        "FULL_TIME",
        BigDecimal.valueOf(2000),
        BigDecimal.valueOf(3000),
        List.of("Java", "Spring"),
        true);
    createVacancy(
        "Dev 2",
        "EPAM",
        "RABOTA_BY",
        "Minsk",
        "PART_TIME",
        BigDecimal.valueOf(1000),
        BigDecimal.valueOf(1500),
        List.of("Java"),
        true);
    createVacancy(
        "Dev 3",
        "Google",
        "HH_RU",
        "Moscow",
        "FULL_TIME",
        BigDecimal.valueOf(5000),
        BigDecimal.valueOf(7000),
        List.of("Go", "Kubernetes"),
        true);

    SearchFilters filters =
        new SearchFilters(
            null,
            null,
            "RABOTA_BY",
            "FULL_TIME",
            BigDecimal.valueOf(1500),
            null,
            List.of("Java"),
            "EPAM",
            null,
            null,
            0,
            20);

    SearchResponse<Vacancy> response = searchService.search(filters).block();

    assertThat(response.total()).isEqualTo(1);
    assertThat(response.results()).hasSize(1);
    assertThat(response.results().get(0).title()).isEqualTo("Dev 1");
  }

  @Test
  void search_pagination_returnsCorrectPage() {
    for (int i = 0; i < 5; i++) {
      createVacancy("Dev " + i, "Company", "SRC", null, "FULL_TIME", null, null, null, true);
    }

    SearchFilters page0 =
        new SearchFilters(null, null, null, null, null, null, null, null, null, null, 0, 2);
    SearchResponse<Vacancy> response0 = searchService.search(page0).block();

    assertThat(response0.total()).isEqualTo(5);
    assertThat(response0.results()).hasSize(2);
    assertThat(response0.page()).isEqualTo(0);
    assertThat(response0.pageSize()).isEqualTo(2);

    SearchFilters page1 =
        new SearchFilters(null, null, null, null, null, null, null, null, null, null, 1, 2);
    SearchResponse<Vacancy> response1 = searchService.search(page1).block();

    assertThat(response1.total()).isEqualTo(5);
    assertThat(response1.results()).hasSize(2);

    SearchFilters page2 =
        new SearchFilters(null, null, null, null, null, null, null, null, null, null, 2, 2);
    SearchResponse<Vacancy> response2 = searchService.search(page2).block();

    assertThat(response2.total()).isEqualTo(5);
    assertThat(response2.results()).hasSize(1);
  }

  @Test
  void search_allFiltersApplied_returnsCorrectCount() {
    UUID id1 =
        createVacancy(
            "Java Dev",
            "EPAM",
            "RABOTA_BY",
            "Minsk",
            "FULL_TIME",
            BigDecimal.valueOf(2000),
            BigDecimal.valueOf(3000),
            List.of("Java", "Spring"),
            true);
    createTranslation(
        id1, "english", "Java Dev", "Senior Java developer position at EPAM in Minsk");

    UUID id2 =
        createVacancy(
            "Java Dev",
            "Other",
            "HH_RU",
            "Moscow",
            "PART_TIME",
            BigDecimal.valueOf(1000),
            BigDecimal.valueOf(1500),
            List.of("Java"),
            true);
    createTranslation(id2, "english", "Java Dev", "Junior Java developer position in Moscow");

    SearchFilters filters =
        new SearchFilters(
            "java",
            "en",
            "RABOTA_BY",
            "FULL_TIME",
            BigDecimal.valueOf(1500),
            BigDecimal.valueOf(5000),
            List.of("Java", "Spring"),
            "EPAM",
            "Minsk",
            false,
            0,
            20);

    SearchResponse<Vacancy> response = searchService.search(filters).block();

    assertThat(response.total()).isEqualTo(1);
    assertThat(response.results()).hasSize(1);
    assertThat(response.results().get(0).id()).isEqualTo(id1);
  }
}
