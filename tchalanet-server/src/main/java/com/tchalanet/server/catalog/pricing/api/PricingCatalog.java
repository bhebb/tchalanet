package com.tchalanet.server.catalog.pricing.api;

import com.tchalanet.server.common.types.enums.BetType;
import com.tchalanet.server.common.types.id.TenantId;
import java.math.BigDecimal;

public interface PricingCatalog {
  BigDecimal oddsFor(TenantId tenantId, String gameCode, BetType betType, Short betOption);
}

