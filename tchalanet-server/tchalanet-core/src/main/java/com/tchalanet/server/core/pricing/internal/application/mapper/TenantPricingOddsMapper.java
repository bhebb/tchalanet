package com.tchalanet.server.core.pricing.internal.application.mapper;

import com.tchalanet.server.core.pricing.api.model.TenantPricingOddsView;
import com.tchalanet.server.core.pricing.internal.domain.TenantPricingOdds;
import org.springframework.stereotype.Component;

@Component
public class TenantPricingOddsMapper {

    public TenantPricingOddsView toView(TenantPricingOdds odds) {
        return new TenantPricingOddsView(
            odds.tenantId(),
            odds.gameCode(),
            odds.pricingVariantCode(),
            odds.betType(),
            odds.betOption(),
            odds.odds(),
            odds.active()
        );
    }
}
