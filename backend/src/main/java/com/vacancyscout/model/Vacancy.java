package com.vacancyscout.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("vacancies")
public record Vacancy(
    @Id UUID id,
    String sourceId,
    String sourceName,
    String title,
    String companyName,
    String companyWebsite,
    String description,
    String requirements,
    String responsibilities,
    BigDecimal salaryMin,
    BigDecimal salaryMax,
    String salaryCurrency,
    String location,
    String employmentType,
    String experienceRequired,
    List<String> skills,
    LocalDateTime postedAt,
    String url,
    boolean isActive,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {
  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private UUID id;
    private String sourceId;
    private String sourceName;
    private String title;
    private String companyName;
    private String companyWebsite;
    private String description;
    private String requirements;
    private String responsibilities;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String salaryCurrency;
    private String location;
    private String employmentType;
    private String experienceRequired;
    private List<String> skills;
    private LocalDateTime postedAt;
    private String url;
    private boolean isActive = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Builder() {}

    public Builder id(UUID id) {
      this.id = id;
      return this;
    }

    public Builder sourceId(String sourceId) {
      this.sourceId = sourceId;
      return this;
    }

    public Builder sourceName(String sourceName) {
      this.sourceName = sourceName;
      return this;
    }

    public Builder title(String title) {
      this.title = title;
      return this;
    }

    public Builder companyName(String companyName) {
      this.companyName = companyName;
      return this;
    }

    public Builder companyWebsite(String companyWebsite) {
      this.companyWebsite = companyWebsite;
      return this;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public Builder requirements(String requirements) {
      this.requirements = requirements;
      return this;
    }

    public Builder responsibilities(String responsibilities) {
      this.responsibilities = responsibilities;
      return this;
    }

    public Builder salaryMin(BigDecimal salaryMin) {
      this.salaryMin = salaryMin;
      return this;
    }

    public Builder salaryMax(BigDecimal salaryMax) {
      this.salaryMax = salaryMax;
      return this;
    }

    public Builder salaryCurrency(String salaryCurrency) {
      this.salaryCurrency = salaryCurrency;
      return this;
    }

    public Builder location(String location) {
      this.location = location;
      return this;
    }

    public Builder employmentType(String employmentType) {
      this.employmentType = employmentType;
      return this;
    }

    public Builder experienceRequired(String experienceRequired) {
      this.experienceRequired = experienceRequired;
      return this;
    }

    public Builder skills(List<String> skills) {
      this.skills = skills;
      return this;
    }

    public Builder postedAt(LocalDateTime postedAt) {
      this.postedAt = postedAt;
      return this;
    }

    public Builder url(String url) {
      this.url = url;
      return this;
    }

    public Builder isActive(boolean isActive) {
      this.isActive = isActive;
      return this;
    }

    public Builder createdAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder updatedAt(LocalDateTime updatedAt) {
      this.updatedAt = updatedAt;
      return this;
    }

    public Vacancy build() {
      return new Vacancy(
          id,
          sourceId,
          sourceName,
          title,
          companyName,
          companyWebsite,
          description,
          requirements,
          responsibilities,
          salaryMin,
          salaryMax,
          salaryCurrency,
          location,
          employmentType,
          experienceRequired,
          skills,
          postedAt,
          url,
          isActive,
          createdAt,
          updatedAt);
    }
  }
}
