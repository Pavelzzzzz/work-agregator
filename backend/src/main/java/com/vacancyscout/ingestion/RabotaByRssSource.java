package com.vacancyscout.ingestion;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Component
public class RabotaByRssSource {

  private static final Logger LOG = LoggerFactory.getLogger(RabotaByRssSource.class);
  private static final Pattern COMPANY_PATTERN =
      Pattern.compile("Вакансия компании:\\s*(.+?)(?:</p>|$)");
  private static final Pattern REGION_PATTERN = Pattern.compile("Регион:\\s*(.+?)(?:</p>|$)");
  private static final Pattern SALARY_PATTERN =
      Pattern.compile("месячного дохода:\\s*(.+?)(?:</p>|$)");
  private static final Pattern ID_PATTERN = Pattern.compile("/vacancy/(\\d+)");

  private final WebClient webClient;
  private final String rssBaseUrl;
  private final int area;

  public RabotaByRssSource(
      @Value("${rabota.rss.base-url}") String rssBaseUrl, @Value("${rabota.rss.area}") int area) {
    this.rssBaseUrl = rssBaseUrl;
    this.area = area;
    this.webClient =
        WebClient.builder()
            .baseUrl(rssBaseUrl)
            .defaultHeader("User-Agent", "VacancyScout/1.0")
            .build();
  }

  public Flux<RssItem> fetch(String query) {
    return webClient
        .get()
        .uri(
            uri ->
                uri.path("/search/vacancy/rss")
                    .queryParam("area", area)
                    .queryParam("text", query)
                    .build())
        .retrieve()
        .bodyToMono(String.class)
        .flatMapMany(xml -> Flux.fromIterable(parseRss(xml)))
        .doOnError(e -> LOG.error("Failed to fetch RSS for query '{}'", query, e))
        .onErrorResume(e -> Flux.empty());
  }

  List<RssItem> parseRss(String xml) {
    var items = new ArrayList<RssItem>();
    try {
      var factory = DocumentBuilderFactory.newInstance();
      var builder = factory.newDocumentBuilder();
      var doc = builder.parse(new org.xml.sax.InputSource(new java.io.StringReader(xml)));
      var itemNodes = doc.getDocumentElement().getElementsByTagName("item");

      for (int i = 0; i < itemNodes.getLength(); i++) {
        var itemEl = itemNodes.item(i);
        var children = itemEl.getChildNodes();
        String title = null;
        String link = null;
        String pubDate = null;
        String description = null;

        for (int j = 0; j < children.getLength(); j++) {
          var child = children.item(j);
          var name = child.getNodeName();
          var text = child.getTextContent();
          if ("title".equals(name)) {
            title = text;
          } else if ("link".equals(name)) {
            link = text;
          } else if ("pubDate".equals(name)) {
            pubDate = text;
          } else if ("description".equals(name)) {
            description = text;
          }
        }

        if (title == null || link == null) {
          continue;
        }

        var vacancyId = extractId(link);
        if (vacancyId == null) {
          continue;
        }

        var company = extractGroup(description, COMPANY_PATTERN);
        var region = extractGroup(description, REGION_PATTERN);
        var salaryRaw = extractGroup(description, SALARY_PATTERN);
        var posted = parseDate(pubDate);

        items.add(new RssItem(vacancyId, title, link, company, region, salaryRaw, posted));
      }
    } catch (Exception e) {
      LOG.error("Failed to parse RSS XML", e);
    }
    return items;
  }

  private String extractId(String link) {
    var m = ID_PATTERN.matcher(link);
    if (m.find()) {
      return m.group(1);
    }
    return null;
  }

  private String extractGroup(String html, Pattern pattern) {
    if (html == null) {
      return null;
    }
    var m = pattern.matcher(html);
    if (m.find()) {
      return m.group(1).trim();
    }
    return null;
  }

  private LocalDateTime parseDate(String dateStr) {
    if (dateStr == null) {
      return null;
    }
    try {
      return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    } catch (Exception e) {
      return null;
    }
  }

  public record RssItem(
      String vacancyId,
      String title,
      String link,
      String companyName,
      String region,
      String salaryRaw,
      LocalDateTime postedAt) {}
}
