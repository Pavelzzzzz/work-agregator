package com.vacancyscout.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vacancyscout.AbstractIntegrationTest;
import com.vacancyscout.dto.SearchFilters;
import com.vacancyscout.dto.SearchResponse;
import com.vacancyscout.model.Vacancy;
import com.vacancyscout.model.VacancyTranslation;
import com.vacancyscout.repository.VacancyRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;

@Tag("integration")
class VacancySearchServiceIntegrationTest extends AbstractIntegrationTest {

  @Autowired private VacancySearchService searchService;

  @Autowired private R2dbcEntityTemplate entityTemplate;

  @Autowired private VacancyRepository vacancyRepository;

  @BeforeEach
  void setUp() {
    entityTemplate
        .delete(VacancyTranslation.class)
        .matching(Query.query(Criteria.empty()))
        .all()
        .then(vacancyRepository.deleteAll())
        .block();
  }

  private Vacancy.Builder aVacancy(String title, String company, String source) {
    return Vacancy.builder()
        .id(UUID.randomUUID())
        .sourceId("src-" + UUID.randomUUID())
        .sourceName(source)
        .title(title)
        .companyName(company)
        .isActive(true)
        .postedAt(LocalDateTime.now())
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now());
  }

  private Vacancy persist(Vacancy.Builder b) {
    return vacancyRepository.save(b.build()).block();
  }

  private void insertTranslation(UUID vacancyId, String lang, String title, String description) {
    entityTemplate
        .insert(VacancyTranslation.class)
        .using(
            new VacancyTranslation(
                UUID.randomUUID(), vacancyId, lang, title, null, description, null, null))
        .block();
  }

  @Test
  void search_withoutFilters_returnsAllActive() {
    persist(
        aVacancy("Dev 1", "Company A", "SOURCE_1")
            .location("Minsk")
            .employmentType("FULL_TIME")
            .salaryMin(BigDecimal.valueOf(1000))
            .salaryMax(BigDecimal.valueOf(2000))
            .skills(List.of("Java")));
    persist(
        aVacancy("Dev 2", "Company B", "SOURCE_2")
            .location("Remote")
            .employmentType("PART_TIME")
            .salaryMin(BigDecimal.valueOf(1500))
            .salaryMax(BigDecimal.valueOf(2500))
            .skills(List.of("Python")));
    persist(
        aVacancy("Dev 3", "Company C", "SOURCE_1")
            .location("Moscow")
            .employmentType("CONTRACT")
            .salaryMin(BigDecimal.valueOf(500))
            .salaryMax(BigDecimal.valueOf(1000))
            .skills(List.of("Go"))
            .isActive(false));

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
    Vacancy v1 =
        persist(
            aVacancy("Java Developer", "Company A", "SOURCE_1")
                .location("Minsk")
                .employmentType("FULL_TIME"));
    insertTranslation(
        v1.id(),
        "english",
        "Java Developer",
        "We need a skilled Java developer with Spring Boot experience");

    Vacancy v2 =
        persist(
            aVacancy("Python Developer", "Company B", "SOURCE_1")
                .location("Minsk")
                .employmentType("FULL_TIME"));
    insertTranslation(
        v2.id(),
        "english",
        "Python Developer",
        "We need a Python developer with Django experience");

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
    persist(
        aVacancy("Dev 1", "Company A", "RABOTA_BY").location("Minsk").employmentType("FULL_TIME"));
    persist(aVacancy("Dev 2", "Company B", "HH_RU").location("Moscow").employmentType("FULL_TIME"));
    persist(
        aVacancy("Dev 3", "Company C", "RABOTA_BY").location("SPB").employmentType("PART_TIME"));

    SearchFilters filters =
        new SearchFilters(null, null, "RABOTA_BY", null, null, null, null, null, null, null, 0, 20);

    SearchResponse<Vacancy> response = searchService.search(filters).block();

    assertThat(response.total()).isEqualTo(2);
    assertThat(response.results()).hasSize(2);
    assertThat(response.results())
        .extracting(Vacancy::sourceName)
        .allMatch(s -> "RABOTA_BY".equals(s));
  }

  @Test
  void search_byEmploymentType_filtersCorrectly() {
    persist(aVacancy("Dev 1", "Company A", "SRC").location("Minsk").employmentType("FULL_TIME"));
    persist(aVacancy("Dev 2", "Company B", "SRC").location("Minsk").employmentType("PART_TIME"));
    persist(aVacancy("Dev 3", "Company C", "SRC").location("Minsk").employmentType("FULL_TIME"));

    SearchFilters filters =
        new SearchFilters(null, null, null, "PART_TIME", null, null, null, null, null, null, 0, 20);

    SearchResponse<Vacancy> response = searchService.search(filters).block();

    assertThat(response.total()).isEqualTo(1);
    assertThat(response.results()).hasSize(1);
    assertThat(response.results().get(0).employmentType()).isEqualTo("PART_TIME");
  }

  @Test
  void search_byCompanyName_filtersCorrectly() {
    persist(aVacancy("Dev 1", "EPAM Systems", "SRC").location("Minsk").employmentType("FULL_TIME"));
    persist(aVacancy("Dev 2", "Google", "SRC").location("Minsk").employmentType("FULL_TIME"));
    persist(
        aVacancy("Dev 3", "EPAM Technologies", "SRC")
            .location("Minsk")
            .employmentType("FULL_TIME"));

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
    persist(aVacancy("Dev 1", "Company A", "SRC").location("Minsk").employmentType("FULL_TIME"));
    persist(aVacancy("Dev 2", "Company B", "SRC").location("Moscow").employmentType("FULL_TIME"));
    persist(
        aVacancy("Dev 3", "Company C", "SRC").location("Minsk City").employmentType("FULL_TIME"));

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
    persist(aVacancy("Dev 1", "Company A", "SRC").location("Minsk").employmentType("FULL_TIME"));
    persist(aVacancy("Dev 2", "Company B", "SRC").employmentType("FULL_TIME"));
    persist(
        aVacancy("Dev 3", "Company C", "SRC").location("Remote OK").employmentType("FULL_TIME"));

    SearchFilters filters =
        new SearchFilters(null, null, null, null, null, null, null, null, null, true, 0, 20);

    SearchResponse<Vacancy> response = searchService.search(filters).block();

    assertThat(response.total()).isEqualTo(1);
    assertThat(response.results()).hasSize(1);
    assertThat(response.results().get(0).location()).isNull();
  }

  @Test
  void search_byMinSalary_filtersCorrectly() {
    persist(
        aVacancy("Dev 1", "Company A", "SRC")
            .employmentType("FULL_TIME")
            .salaryMin(BigDecimal.valueOf(1000))
            .salaryMax(BigDecimal.valueOf(2000)));
    persist(
        aVacancy("Dev 2", "Company B", "SRC")
            .employmentType("FULL_TIME")
            .salaryMin(BigDecimal.valueOf(3000))
            .salaryMax(BigDecimal.valueOf(4000)));
    persist(
        aVacancy("Dev 3", "Company C", "SRC")
            .employmentType("FULL_TIME")
            .salaryMin(BigDecimal.valueOf(500))
            .salaryMax(BigDecimal.valueOf(800)));

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
    persist(
        aVacancy("Dev 1", "Company A", "SRC")
            .employmentType("FULL_TIME")
            .salaryMin(BigDecimal.valueOf(1000))
            .salaryMax(BigDecimal.valueOf(2000)));
    persist(
        aVacancy("Dev 2", "Company B", "SRC")
            .employmentType("FULL_TIME")
            .salaryMin(BigDecimal.valueOf(3000))
            .salaryMax(BigDecimal.valueOf(4000)));
    persist(
        aVacancy("Dev 3", "Company C", "SRC")
            .employmentType("FULL_TIME")
            .salaryMin(BigDecimal.valueOf(500))
            .salaryMax(BigDecimal.valueOf(800)));

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
    persist(
        aVacancy("Dev 1", "Company A", "SRC")
            .employmentType("FULL_TIME")
            .skills(List.of("Java", "Spring", "PostgreSQL")));
    persist(
        aVacancy("Dev 2", "Company B", "SRC")
            .employmentType("FULL_TIME")
            .skills(List.of("Python", "Django")));
    persist(
        aVacancy("Dev 3", "Company C", "SRC")
            .employmentType("FULL_TIME")
            .skills(List.of("Java", "React")));

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
    persist(
        aVacancy("Dev 1", "EPAM", "RABOTA_BY")
            .location("Minsk")
            .employmentType("FULL_TIME")
            .salaryMin(BigDecimal.valueOf(2000))
            .salaryMax(BigDecimal.valueOf(3000))
            .skills(List.of("Java", "Spring")));
    persist(
        aVacancy("Dev 2", "EPAM", "RABOTA_BY")
            .location("Minsk")
            .employmentType("PART_TIME")
            .salaryMin(BigDecimal.valueOf(1000))
            .salaryMax(BigDecimal.valueOf(1500))
            .skills(List.of("Java")));
    persist(
        aVacancy("Dev 3", "Google", "HH_RU")
            .location("Moscow")
            .employmentType("FULL_TIME")
            .salaryMin(BigDecimal.valueOf(5000))
            .salaryMax(BigDecimal.valueOf(7000))
            .skills(List.of("Go", "Kubernetes")));

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
      persist(aVacancy("Dev " + i, "Company", "SRC").employmentType("FULL_TIME"));
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
    Vacancy v1 =
        persist(
            aVacancy("Java Dev", "EPAM", "RABOTA_BY")
                .location("Minsk")
                .employmentType("FULL_TIME")
                .salaryMin(BigDecimal.valueOf(2000))
                .salaryMax(BigDecimal.valueOf(3000))
                .skills(List.of("Java", "Spring")));
    insertTranslation(
        v1.id(), "english", "Java Dev", "Senior Java developer position at EPAM in Minsk");

    Vacancy v2 =
        persist(
            aVacancy("Java Dev", "Other", "HH_RU")
                .location("Moscow")
                .employmentType("PART_TIME")
                .salaryMin(BigDecimal.valueOf(1000))
                .salaryMax(BigDecimal.valueOf(1500))
                .skills(List.of("Java")));
    insertTranslation(v2.id(), "english", "Java Dev", "Junior Java developer position in Moscow");

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
    assertThat(response.results().get(0).id()).isEqualTo(v1.id());
  }
}
