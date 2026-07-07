package com.tchalanet.server.core.pricing.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.pricing.api.model.TenantPricingOddsView;
import java.util.List;

public record ListTenantPricingQuery(
    TenantId tenantId,
    String gameCode
) implements Query<List<TenantPricingOddsView>> {}
