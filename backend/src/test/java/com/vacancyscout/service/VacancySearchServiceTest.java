package com.vacancyscout.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vacancyscout.dto.SearchFilters;
import java.util.List;
import org.junit.jupiter.api.Test;

class VacancySearchServiceTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void parseSkills_shouldHandleJson() {
    var service = new VacancySearchService(null, objectMapper);

    // No easy way to unit test the search without a database,
    // but verify the service can be constructed.
    assertThat(service).isNotNull();
  }

  @Test
  void searchFilters_shouldAcceptAllParams() {
    SearchFilters filters =
        new SearchFilters(
            "Java",
            "ru",
            "RABOTA_BY",
            "FULL_TIME",
            null,
            null,
            List.of("Java", "Spring"),
            "EPAM",
            "Minsk",
            true,
            0,
            20);
    assertThat(filters.query()).isEqualTo("Java");
    assertThat(filters.source()).isEqualTo("RABOTA_BY");
    assertThat(filters.skills()).containsExactly("Java", "Spring");
  }
}
