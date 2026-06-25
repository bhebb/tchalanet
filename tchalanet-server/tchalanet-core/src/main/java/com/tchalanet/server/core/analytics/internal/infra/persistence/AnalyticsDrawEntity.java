package com.tchalanet.server.core.analytics.internal.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** JPA mapping for {@code analytics_draw}. */
@Entity
@Table(name = "analytics_draw")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsDrawEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "draw_id", nullable = false, unique = true, columnDefinition = "uuid")
  private UUID drawId;

  @Column(name = "tenant_id", nullable = false, columnDefinition = "uuid")
  private UUID tenantId;

  @Column(name = "game_code", nullable = false)
  private String gameCode;

  @Column(name = "draw_channel_code")
  private String drawChannelCode;

  @Column(name = "scheduled_at", nullable = false)
  private Instant scheduledAt;

  @Column(name = "ref_date", nullable = false)
  private LocalDate refDate;

  @Column(name = "tickets_sold_count", nullable = false)
  private long ticketsSoldCount;

  @Column(name = "tickets_cancelled_count", nullable = false)
  private long ticketsCancelledCount;

  @Column(name = "gross_sales_cents", nullable = false)
  private long grossSalesCents;

  @Column(name = "stake_total_cents", nullable = false)
  private long stakeTotalCents;

  @Column(name = "winnings_calculated_cents", nullable = false)
  private long winningsCalculatedCents;

  @Column(name = "payouts_paid_cents", nullable = false)
  private long payoutsPaidCents;

  @Column(name = "seller_commission_cents", nullable = false)
  private long sellerCommissionCents;

  @Column(name = "buyer_charge_cents", nullable = false)
  private long buyerChargeCents;

  @Column(name = "seller_charge_cents", nullable = false)
  private long sellerChargeCents;

  @Column(name = "tenant_charge_cents", nullable = false)
  private long tenantChargeCents;

  @Column(name = "waived_charge_cents", nullable = false)
  private long waivedChargeCents;

  @Column(name = "promotion_line_count", nullable = false)
  private long promotionLineCount;

  @Column(name = "promotion_priced_line_count", nullable = false)
  private long promotionPricedLineCount;

  @Column(name = "promotion_payout_base_cents", nullable = false)
  private long promotionPayoutBaseCents;

  @Column(name = "promotion_potential_payout_cents", nullable = false)
  private long promotionPotentialPayoutCents;

  @Column(name = "net_revenue_estimated_cents", nullable = false)
  private long netRevenueEstimatedCents;

  @Column(name = "net_revenue_paid_basis_cents", nullable = false)
  private long netRevenuePaidBasisCents;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
