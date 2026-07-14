package com.tchalanet.server.core.pricing.internal.application.port.out;

import com.tchalanet.server.core.pricing.internal.domain.TenantPricingOdds;

public interface TenantPricingOddsWriterPort {
  TenantPricingOdds save(TenantPricingOdds odds);
}
