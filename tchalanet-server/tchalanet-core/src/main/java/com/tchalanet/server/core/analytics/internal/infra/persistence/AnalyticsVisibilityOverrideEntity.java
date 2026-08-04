package com.tchalanet.server.core.analytics.internal.infra.persistence;

import com.tchalanet.server.core.analytics.api.model.AnalyticsTrustScopeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Persisted platform-ops kill-switch for financial analytics visibility. */
@Entity
@Table(name = "analytics_visibility_override")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsVisibilityOverrideEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(name = "scope_type", nullable = false, length = 32)
  private AnalyticsTrustScopeType scopeType;

  @Column(name = "tenant_id", columnDefinition = "uuid")
  private UUID tenantId;

  @Column(name = "seller_terminal_id", columnDefinition = "uuid")
  private UUID sellerTerminalId;

  @Column(name = "draw_id", columnDefinition = "uuid")
  private UUID drawId;

  @Column(name = "from_date", nullable = false)
  private LocalDate fromDate;

  @Column(name = "to_date", nullable = false)
  private LocalDate toDate;

  @Column(name = "reason", nullable = false, length = 500)
  private String reason;

  @Column(name = "changed_by", columnDefinition = "uuid", nullable = false)
  private UUID changedBy;

  @Column(name = "changed_at", nullable = false)
  private Instant changedAt;
}
