package com.vacancyscout.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vacancyscout.dto.SearchFilters;
import com.vacancyscout.repository.VacancyRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class VacancySearchServiceTest {

  private final VacancyRepository vacancyRepository = Mockito.mock(VacancyRepository.class);

  @Test
  void search_shouldDelegateToRepository() {
    var service = new VacancySearchService(vacancyRepository);
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
