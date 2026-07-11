package com.vacancyscout.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RabotaByVacancyDetailFetcherTest {

  private RabotaByVacancyDetailFetcher fetcher;

  @BeforeEach
  void setUp() {
    fetcher = new RabotaByVacancyDetailFetcher(new ObjectMapper());
  }

  @Test
  void parseHtml_validPage_extractsAllFields() throws IOException {
    String html =
        Files.readString(
            Path.of("src/test/resources/rabota/sample-vacancy.html"), StandardCharsets.UTF_8);

    var detail = fetcher.parseHtml(html);

    assertThat(detail).isNotNull();
    assertThat(detail.id()).isEqualTo("12345678");
    assertThat(detail.title()).isEqualTo("Java Developer");
    assertThat(detail.companyName()).isEqualTo("EPAM Systems");
    assertThat(detail.locality()).isEqualTo("Минск");
    assertThat(detail.country()).isEqualTo("Беларусь");
    assertThat(detail.datePosted()).isEqualTo("2025-07-01");
    assertThat(detail.employmentType()).isEqualTo("FULLTIME");
  }

  @Test
  void parseHtml_validPage_extractsDescription() throws IOException {
    String html =
        Files.readString(
            Path.of("src/test/resources/rabota/sample-vacancy.html"), StandardCharsets.UTF_8);

    var detail = fetcher.parseHtml(html);

    assertThat(detail.description()).isNotNull();
    assertThat(detail.description()).contains("Java");
  }

  @Test
  void parseHtml_validPage_extractsResponsibilities() throws IOException {
    String html =
        Files.readString(
            Path.of("src/test/resources/rabota/sample-vacancy.html"), StandardCharsets.UTF_8);

    var detail = fetcher.parseHtml(html);

    assertThat(detail.responsibilities()).contains("Разработка");
    assertThat(detail.responsibilities()).contains("Code review");
  }

  @Test
  void parseHtml_validPage_extractsRequirements() throws IOException {
    String html =
        Files.readString(
            Path.of("src/test/resources/rabota/sample-vacancy.html"), StandardCharsets.UTF_8);

    var detail = fetcher.parseHtml(html);

    assertThat(detail.requirements()).isNotNull();
  }

  @Test
  void parseHtml_validPage_extractsSkills() throws IOException {
    String html =
        Files.readString(
            Path.of("src/test/resources/rabota/sample-vacancy.html"), StandardCharsets.UTF_8);

    var detail = fetcher.parseHtml(html);

    assertThat(detail.skills()).contains("Java", "Spring", "PostgreSQL", "Docker");
  }

  @Test
  void parseHtml_noJsonLd_returnsNull() {
    String html = "<html><head><title>Test</title></head><body></body></html>";

    var detail = fetcher.parseHtml(html);

    assertThat(detail).isNull();
  }

  @Test
  void parseHtml_emptyHtml_returnsNull() {
    var detail = fetcher.parseHtml("");

    assertThat(detail).isNull();
  }

  @Test
  void parseHtml_malformedJsonLd_returnsNull() {
    String html =
        "<html><head>"
            + "<script type=\"application/ld+json\">{invalid json</script>"
            + "</head><body></body></html>";

    var detail = fetcher.parseHtml(html);

    assertThat(detail).isNull();
  }

  @Test
  void parseHtml_jsonLdWithMissingFields_returnsPartialDetail() {
    String html =
        "<html><head>"
            + "<script type=\"application/ld+json\">"
            + "{\"@type\":\"JobPosting\",\"title\":\"Frontend Dev\"}"
            + "</script>"
            + "</head><body></body></html>";

    var detail = fetcher.parseHtml(html);

    assertThat(detail).isNotNull();
    assertThat(detail.title()).isEqualTo("Frontend Dev");
    assertThat(detail.id()).isEmpty();
    assertThat(detail.companyName()).isNull();
    assertThat(detail.locality()).isNull();
    assertThat(detail.skills()).isEmpty();
  }

  @Test
  void parseHtml_skillsExtraction_onlyKnownTech() {
    String jsonLd =
        "{\"@type\":\"JobPosting\",\"title\":\"Dev\","
            + "\"description\":\"<p>Нужен <strong>Задачи</strong></p>"
            + "<ul><li>Писать код на Java и Python</li></ul>"
            + "<p><strong>Требования</strong></p>"
            + "<ul><li>React, Docker, Kubernetes</li></ul>\"}";
    String html =
        "<html><head>"
            + "<script type=\"application/ld+json\">"
            + jsonLd
            + "</script>"
            + "</head><body></body></html>";

    var detail = fetcher.parseHtml(html);

    assertThat(detail).isNotNull();
    assertThat(detail.skills()).contains("Java", "Python", "React", "Docker", "Kubernetes");
    assertThat(detail.skills()).doesNotContain("код");
  }

  @Test
  void parseHtml_duplicateSkills_notDuplicated() {
    String jsonLd =
        "{\"@type\":\"JobPosting\",\"title\":\"Dev\","
            + "\"description\":\"<p>Java experience. More Java. Also Spring.</p>\"}";
    String html =
        "<html><head>"
            + "<script type=\"application/ld+json\">"
            + jsonLd
            + "</script>"
            + "</head><body></body></html>";

    var detail = fetcher.parseHtml(html);

    assertThat(detail).isNotNull();
    long javaCount = detail.skills().stream().filter(s -> s.equals("Java")).count();
    assertThat(javaCount).isEqualTo(1);
  }
}
