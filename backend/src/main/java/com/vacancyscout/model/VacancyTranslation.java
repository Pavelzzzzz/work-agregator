package com.vacancyscout.model;

import org.springframework.data.relational.core.mapping.Table;
import java.util.UUID;

@Table("vacancy_translations")
public record VacancyTranslation(
    UUID vacancyId,
    String lang,
    String title,
    String companyName,
    String description,
    String requirements,
    String responsibilities,
    String searchVector
) {}
