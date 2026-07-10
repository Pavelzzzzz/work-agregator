package com.vacancyscout.model;

import java.util.UUID;
import org.springframework.data.relational.core.mapping.Table;

@Table("vacancy_translations")
public record VacancyTranslation(
    UUID vacancyId,
    String lang,
    String title,
    String companyName,
    String description,
    String requirements,
    String responsibilities,
    String searchVector) {}
