package com.tchalanet.server.catalog.pricing.api;

import com.tchalanet.server.common.types.id.TenantId;

public interface PricingProvisioningApi {

  void ensureDefaultHaitiLotteryOdds(TenantId tenantId);
}
