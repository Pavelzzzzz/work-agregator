package com.vacancyscout.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

@Table("companies")
public class Company implements Persistable<UUID> {
  @Id private UUID id;
  @Transient private boolean _new;
  private String name;
  private String careersUrl;
  private String websiteUrl;
  private String description;
  private boolean isActive;
  private LocalDateTime lastScanAt;
  private String scanStatus;
  private String scanError;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public Company() {}

  public Company(
      UUID id,
      String name,
      String careersUrl,
      String websiteUrl,
      String description,
      boolean isActive,
      LocalDateTime lastScanAt,
      String scanStatus,
      String scanError,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this._new = true;
    this.name = name;
    this.careersUrl = careersUrl;
    this.websiteUrl = websiteUrl;
    this.description = description;
    this.isActive = isActive;
    this.lastScanAt = lastScanAt;
    this.scanStatus = scanStatus;
    this.scanError = scanError;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  @Override
  public UUID getId() {
    return id;
  }

  @Override
  @JsonIgnore
  @Transient
  public boolean isNew() {
    return _new;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public void setNew(boolean isNew) {
    this._new = isNew;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCareersUrl() {
    return careersUrl;
  }

  public void setCareersUrl(String careersUrl) {
    this.careersUrl = careersUrl;
  }

  public String getWebsiteUrl() {
    return websiteUrl;
  }

  public void setWebsiteUrl(String websiteUrl) {
    this.websiteUrl = websiteUrl;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public boolean isActive() {
    return isActive;
  }

  public void setActive(boolean active) {
    isActive = active;
  }

  public LocalDateTime getLastScanAt() {
    return lastScanAt;
  }

  public void setLastScanAt(LocalDateTime lastScanAt) {
    this.lastScanAt = lastScanAt;
  }

  public String getScanStatus() {
    return scanStatus;
  }

  public void setScanStatus(String scanStatus) {
    this.scanStatus = scanStatus;
  }

  public String getScanError() {
    return scanError;
  }

  public void setScanError(String scanError) {
    this.scanError = scanError;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Company company = (Company) o;
    return Objects.equals(id, company.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private UUID id;
    private String name;
    private String careersUrl;
    private String websiteUrl;
    private String description;
    private boolean isActive = true;
    private LocalDateTime lastScanAt;
    private String scanStatus;
    private String scanError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Builder() {}

    public Builder id(UUID v) {
      this.id = v;
      return this;
    }

    public Builder name(String v) {
      this.name = v;
      return this;
    }

    public Builder careersUrl(String v) {
      this.careersUrl = v;
      return this;
    }

    public Builder websiteUrl(String v) {
      this.websiteUrl = v;
      return this;
    }

    public Builder description(String v) {
      this.description = v;
      return this;
    }

    public Builder isActive(boolean v) {
      this.isActive = v;
      return this;
    }

    public Builder lastScanAt(LocalDateTime v) {
      this.lastScanAt = v;
      return this;
    }

    public Builder scanStatus(String v) {
      this.scanStatus = v;
      return this;
    }

    public Builder scanError(String v) {
      this.scanError = v;
      return this;
    }

    public Builder createdAt(LocalDateTime v) {
      this.createdAt = v;
      return this;
    }

    public Builder updatedAt(LocalDateTime v) {
      this.updatedAt = v;
      return this;
    }

    public Company build() {
      return new Company(
          id,
          name,
          careersUrl,
          websiteUrl,
          description,
          isActive,
          lastScanAt,
          scanStatus,
          scanError,
          createdAt,
          updatedAt);
    }
  }
}
