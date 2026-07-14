package com.tchalanet.server.core.pricing.internal.application.port.out;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.pricing.api.model.PricingVariantCode;
import com.tchalanet.server.core.pricing.internal.domain.TenantPricingOdds;
import java.util.List;
import java.util.Optional;

public interface TenantPricingOddsReaderPort {

  List<TenantPricingOdds> findActiveByTenant(TenantId tenantId, String gameCode);

  Optional<TenantPricingOdds> findByNaturalKey(
      TenantId tenantId, String gameCode, PricingVariantCode pricingVariantCode);
}
