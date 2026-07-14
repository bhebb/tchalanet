package com.tchalanet.server.core.pricing.internal.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.core.pricing.api.model.PayoutRuleType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "seller_terminal_pricing_odds_override",
    indexes = {
      @Index(name = "idx_st_pricing_odds_tenant", columnList = "tenant_id"),
      @Index(name = "idx_st_pricing_odds_seller_terminal", columnList = "seller_terminal_id")
    })
@Getter
@Setter
public class SellerTerminalOddsOverrideJpaEntity extends BaseTenantEntity {

  @Column(name = "seller_terminal_id", nullable = false)
  private UUID sellerTerminalId;

  @Column(name = "game_code", nullable = false, length = 64)
  private String gameCode;

  @Column(name = "pricing_variant_code", nullable = false, length = 64)
  private String pricingVariantCode;

  @Column(name = "bet_type", nullable = false, length = 32)
  private String betType;

  @Column(name = "bet_option")
  private Short betOption;

  @Column(name = "odds", nullable = false, precision = 12, scale = 4)
  private BigDecimal odds;

  @Enumerated(EnumType.STRING)
  @Column(name = "payout_rule_type", nullable = false, length = 32)
  private PayoutRuleType payoutRuleType = PayoutRuleType.STAKE_MULTIPLIER;

  @Column(name = "fixed_amount", precision = 19, scale = 4)
  private BigDecimal fixedAmount;

  @Column(name = "active", nullable = false)
  private boolean active = true;

  @Column(name = "effective_from")
  private Instant effectiveFrom;

  @Column(name = "effective_to")
  private Instant effectiveTo;

  @Column(name = "reason", length = 500)
  private String reason;
}
