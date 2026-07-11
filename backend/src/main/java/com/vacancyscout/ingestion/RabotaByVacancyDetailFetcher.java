package com.vacancyscout.ingestion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class RabotaByVacancyDetailFetcher {

  private static final Logger LOG = LoggerFactory.getLogger(RabotaByVacancyDetailFetcher.class);
  private static final Pattern JSON_LD_PATTERN =
      Pattern.compile(
          "<script[^>]*type=\"application/ld\\+json\"[^>]*>(.*?)</script>", Pattern.DOTALL);

  private final WebClient webClient;
  private final ObjectMapper objectMapper;

  public RabotaByVacancyDetailFetcher(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    this.webClient = WebClient.builder().defaultHeader("User-Agent", "VacancyScout/1.0").build();
  }

  public Mono<VacancyDetail> fetch(String url) {
    return webClient
        .get()
        .uri(url)
        .retrieve()
        .bodyToMono(String.class)
        .map(this::parseHtml)
        .doOnError(e -> LOG.error("Failed to fetch vacancy detail: {}", url, e))
        .onErrorResume(e -> Mono.empty());
  }

  VacancyDetail parseHtml(String html) {
    var m = JSON_LD_PATTERN.matcher(html);
    if (!m.find()) {
      return null;
    }

    try {
      var root = objectMapper.readTree(m.group(1));
      var title = path(root, "title");
      var company = path(root, "hiringOrganization", "name");
      var locality = path(root, "jobLocation", "address", "addressLocality");
      var country = path(root, "applicantLocationRequirements", "name");
      var datePosted = path(root, "datePosted");
      var description = path(root, "description");
      var identifier = path(root, "identifier", "value");
      var employmentType = path(root, "employmentType");

      var requirements = new StringBuilder();
      var responsibilities = new StringBuilder();
      var skills = new ArrayList<String>();

      if (description != null) {
        var reqM =
            Pattern.compile(
                    "(?:требовани|пожелани).*?</p>\\s*<ul>(.*?)</ul>",
                    Pattern.DOTALL | Pattern.CASE_INSENSITIVE)
                .matcher(description);
        if (reqM.find()) {
          requirements.append(reqM.group(1).replaceAll("<[^>]+>", "").trim());
        }

        var respM =
            Pattern.compile("<p><strong>(.*?)</strong></p>\\s*<ul>(.*?)</ul>", Pattern.DOTALL)
                .matcher(description);
        while (respM.find()) {
          var header = respM.group(1).toLowerCase(java.util.Locale.ROOT);
          if (header.contains("предстоит")
              || header.contains("обязанност")
              || header.contains("задача")) {
            responsibilities.append(respM.group(2).replaceAll("<[^>]+>", "").trim());
          }
        }

        var skillM =
            Pattern.compile(
                    "(?:Java|Python|JavaScript|TypeScript|SQL|React|Angular|Vue|Spring|Docker"
                        + "|Kubernetes|AWS|Azure|GCP|Git|Linux|PostgreSQL|MongoDB|Redis|Kafka"
                        + "|Node\\.?js|Go|Rust|C\\+\\+|C#|PHP|Ruby|Scala|Kotlin|Swift|Flutter"
                        + "|Terraform|Ansible|Jenkins|CI/CD|Machine Learning|Deep Learning|NLP"
                        + "|TensorFlow|PyTorch|Hadoop|Spark|NoSQL|REST|GraphQL|UX|UI|Figma|Agile|Scrum)",
                    Pattern.CASE_INSENSITIVE)
                .matcher(description);
        while (skillM.find()) {
          var skill = skillM.group().trim();
          if (!skills.contains(skill)) {
            skills.add(skill);
          }
        }
      }

      return new VacancyDetail(
          identifier != null ? identifier : "",
          title,
          company,
          locality,
          country,
          datePosted,
          employmentType,
          description,
          requirements.toString(),
          responsibilities.toString(),
          skills);
    } catch (Exception e) {
      LOG.error("Failed to parse JSON-LD", e);
      return null;
    }
  }

  private String path(JsonNode node, String... fields) {
    for (var f : fields) {
      if (node == null) {
        return null;
      }
      node = node.get(f);
    }
    if (node != null) {
      return node.asText(null);
    }
    return null;
  }

  public record VacancyDetail(
      String id,
      String title,
      String companyName,
      String locality,
      String country,
      String datePosted,
      String employmentType,
      String description,
      String requirements,
      String responsibilities,
      List<String> skills) {}
}
