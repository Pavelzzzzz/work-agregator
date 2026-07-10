package com.vacancyscout.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.util.UUID;
import java.time.LocalDateTime;

@Table("companies")
public record Company(
    @Id UUID id,
    String name,
    String careersUrl,
    String websiteUrl,
    String description,
    boolean isActive,
    LocalDateTime lastScanAt,
    String scanStatus,
    String scanError,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
