package com.vacancyscout.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RabotaByRssSourceTest {

  private RabotaByRssSource source;

  @BeforeEach
  void setUp() {
    source = new RabotaByRssSource("https://rabota.by", 156);
  }

  @Test
  void parseRss_validFeed_extractsAllItems() throws IOException {
    String xml =
        Files.readString(
            Path.of("src/test/resources/rabota/sample-rss.xml"), StandardCharsets.UTF_8);

    var items = source.parseRss(xml);

    assertThat(items).hasSize(3);
  }

  @Test
  void parseRss_validFeed_extractsFieldsCorrectly() throws IOException {
    String xml =
        Files.readString(
            Path.of("src/test/resources/rabota/sample-rss.xml"), StandardCharsets.UTF_8);

    var items = source.parseRss(xml);
    var first = items.get(0);

    assertThat(first.vacancyId()).isEqualTo("12345678");
    assertThat(first.title()).isEqualTo("Java Developer");
    assertThat(first.link()).isEqualTo("https://rabota.by/vacancy/12345678");
    assertThat(first.companyName()).isEqualTo("EPAM Systems");
    assertThat(first.region()).isEqualTo("Минск");
    assertThat(first.salaryRaw()).isEqualTo("3000 - 5000 BYN");
    assertThat(first.postedAt()).isNotNull();
  }

  @Test
  void parseRss_secondItem_extractsCorrectly() throws IOException {
    String xml =
        Files.readString(
            Path.of("src/test/resources/rabota/sample-rss.xml"), StandardCharsets.UTF_8);

    var items = source.parseRss(xml);
    var second = items.get(1);

    assertThat(second.vacancyId()).isEqualTo("87654321");
    assertThat(second.title()).isEqualTo("Python Developer");
    assertThat(second.companyName()).isEqualTo("Wargaming");
    assertThat(second.region()).isEqualTo("Минск");
  }

  @Test
  void parseRss_itemWithoutSalary_salaryIsNull() throws IOException {
    String xml =
        Files.readString(
            Path.of("src/test/resources/rabota/sample-rss.xml"), StandardCharsets.UTF_8);

    var items = source.parseRss(xml);
    var third = items.get(2);

    assertThat(third.salaryRaw()).isEqualTo("не указан");
    assertThat(third.region()).isEqualTo("Гомель");
  }

  @Test
  void parseRss_emptyXml_returnsEmptyList() {
    String xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><rss version=\"2.0\"><channel></channel></rss>";

    var items = source.parseRss(xml);

    assertThat(items).isEmpty();
  }

  @Test
  void parseRss_invalidXml_returnsEmptyList() {
    String xml = "this is not xml at all {{{";

    var items = source.parseRss(xml);

    assertThat(items).isEmpty();
  }

  @Test
  void parseRss_itemWithoutId_returnsEmptyList() {
    String xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<rss version=\"2.0\"><channel>"
            + "<item>"
            + "<title>No Link</title>"
            + "<link>https://rabota.by/some-page</link>"
            + "<description>no id here</description>"
            + "</item>"
            + "</channel></rss>";

    var items = source.parseRss(xml);

    assertThat(items).isEmpty();
  }

  @Test
  void parseRss_itemWithNoTitleOrLink_isSkipped() {
    String xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<rss version=\"2.0\"><channel>"
            + "<item>"
            + "<description>Missing title and link</description>"
            + "</item>"
            + "</channel></rss>";

    var items = source.parseRss(xml);

    assertThat(items).isEmpty();
  }

  @Test
  void parseRss_minimalItem_extractsOnlyIdAndTitle() {
    String xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<rss version=\"2.0\"><channel>"
            + "<item>"
            + "<title>Minimal Vacancy</title>"
            + "<link>https://rabota.by/vacancy/99999999</link>"
            + "</item>"
            + "</channel></rss>";

    var items = source.parseRss(xml);

    assertThat(items).hasSize(1);
    var item = items.get(0);
    assertThat(item.vacancyId()).isEqualTo("99999999");
    assertThat(item.title()).isEqualTo("Minimal Vacancy");
    assertThat(item.companyName()).isNull();
    assertThat(item.region()).isNull();
    assertThat(item.salaryRaw()).isNull();
    assertThat(item.postedAt()).isNull();
  }

  @Test
  void parseRss_malformedDate_postedAtIsNull() {
    String xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<rss version=\"2.0\"><channel>"
            + "<item>"
            + "<title>Test</title>"
            + "<link>https://rabota.by/vacancy/11111111</link>"
            + "<pubDate>not-a-date</pubDate>"
            + "</item>"
            + "</channel></rss>";

    var items = source.parseRss(xml);

    assertThat(items).hasSize(1);
    assertThat(items.get(0).postedAt()).isNull();
  }
}
