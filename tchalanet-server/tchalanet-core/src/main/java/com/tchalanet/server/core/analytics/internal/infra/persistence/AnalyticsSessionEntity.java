package com.tchalanet.server.core.analytics.internal.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "analytics_session")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsSessionEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "sales_session_id", columnDefinition = "uuid", nullable = false, unique = true)
  private UUID salesSessionId;

  @Column(name = "tenant_id", columnDefinition = "uuid", nullable = false)
  private UUID tenantId;

  @Column(name = "outlet_id", columnDefinition = "uuid")
  private UUID outletId;

  @Column(name = "terminal_id", columnDefinition = "uuid")
  private UUID terminalId;

  @Column(name = "seller_user_id", columnDefinition = "uuid")
  private UUID sellerUserId;

  @Column(name = "business_date", nullable = false)
  private LocalDate businessDate;

  @Column(name = "tickets_sold_count", nullable = false)
  private long ticketsSoldCount;

  @Column(name = "tickets_voided_count", nullable = false)
  private long ticketsVoidedCount;

  @Column(name = "gross_sales_cents", nullable = false)
  private long grossSalesCents;

  @Column(name = "stake_total_cents", nullable = false)
  private long stakeTotalCents;

  @Column(name = "payouts_paid_cents", nullable = false)
  private long payoutsPaidCents;

  @Column(name = "status", nullable = false, length = 32)
  private String status;

  @Column(name = "opened_at")
  private Instant openedAt;

  @Column(name = "closed_at")
  private Instant closedAt;

  @Version
  @Column(name = "version", nullable = false)
  private long version;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
