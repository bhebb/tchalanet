package com.tchalanet.server.core.pricing.internal.application.mapper;

import com.tchalanet.server.core.pricing.api.model.TenantPricingRuleView;
import com.tchalanet.server.core.pricing.internal.domain.TenantPricingOdds;
import org.springframework.stereotype.Component;

@Component
public class TenantPricingOddsMapper {

  public TenantPricingRuleView toView(TenantPricingOdds odds) {
    return new TenantPricingRuleView(
        odds.tenantId(),
        odds.gameCode(),
        odds.pricingVariantCode(),
        odds.betType(),
        odds.betOption(),
        odds.odds(),
        odds.payoutRuleType(),
        odds.fixedAmount(),
        odds.active());
  }
}
