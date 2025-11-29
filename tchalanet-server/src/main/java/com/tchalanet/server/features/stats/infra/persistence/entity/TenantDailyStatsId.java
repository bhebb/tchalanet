package com.tchalanet.server.features.stats.infra.persistence.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/** Composite primary key for TenantDailyStatsEntity. */
public class TenantDailyStatsId implements Serializable {
  private UUID tenantId;
  private LocalDate day;

  // Default constructor for JPA
  public TenantDailyStatsId() {}

  public TenantDailyStatsId(UUID tenantId, LocalDate day) {
    this.tenantId = tenantId;
    this.day = day;
  }

  // Getters, Setters, equals, and hashCode
  public UUID getTenantId() {
    return tenantId;
  }

  public void setTenantId(UUID tenantId) {
    this.tenantId = tenantId;
  }

  public LocalDate getDay() {
    return day;
  }

  public void setDay(LocalDate day) {
    this.day = day;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TenantDailyStatsId that = (TenantDailyStatsId) o;
    return Objects.equals(tenantId, that.tenantId) && Objects.equals(day, that.day);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tenantId, day);
  }
}
