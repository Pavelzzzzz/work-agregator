package com.vacancyscout.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record VacancyUpdateEvent(
    UUID id,
    UUID vacancyId,
    String title,
    String companyName,
    LocalDateTime postedAt,
    String sourceName,
    String eventType, // NEW | UPDATED
    String source
) {}
