package com.vacancyscout.dto;

import java.math.BigDecimal;
import java.util.List;

public record SearchFilters(
    String query,
    String language,
    String source,
    String employmentType,
    BigDecimal minSalary,
    BigDecimal maxSalary,
    List<String> skills,
    String companyName,
    String location,
    Boolean remoteOnly,
    Integer page,
    Integer pageSize) {}
