package com.tchalanet.server.core.pricing.internal.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.core.pricing.api.model.PayoutRuleType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "pricing_odds")
@Getter
@Setter
public class TenantPricingOddsJpaEntity extends BaseTenantEntity {

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
}
