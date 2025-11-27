package com.tchalanet.server.stats.infra.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tenant_daily_stats")
@Getter
@Setter
@IdClass(TenantDailyStatsId.class) // Composite primary key
public class TenantDailyStatsEntity {

  @Id
  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @Id
  @Column(name = "day", nullable = false)
  private LocalDate day;

  @Column(name = "total_tickets", nullable = false)
  private long totalTickets;

  @Column(name = "total_stake", nullable = false)
  private BigDecimal totalStake;

  @Column(name = "total_payout", nullable = false)
  private BigDecimal totalPayout;

  @Column(name = "gross_margin", nullable = false)
  private BigDecimal grossMargin;

  @Column(name = "margin_pct", nullable = false)
  private BigDecimal marginPct;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = Instant.now();
    updatedAt = Instant.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = Instant.now();
  }
}
