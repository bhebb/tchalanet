package com.tchalanet.server.catalog.pricing.api;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.pricing.api.model.PricingStatsView;
import com.tchalanet.server.catalog.pricing.internal.web.model.PricingOddsView;
import com.tchalanet.server.common.types.id.TenantId;

import java.math.BigDecimal;
import java.util.List;

public interface PricingCatalog {
    BigDecimal oddsFor(TenantId tenantId, String gameCode, BetType betType, Short betOption);

    List<PricingOddsView> getOdds(TenantId tenantId);

    // NEW: stats for platform admin
    PricingStatsView stats();
}
