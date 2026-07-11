package com.tchalanet.server.core.pricing.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.pricing.api.model.TenantPricingRuleView;
import java.util.List;

public record ListTenantPricingRulesQuery(
    TenantId tenantId,
    String gameCode
) implements Query<List<TenantPricingRuleView>> {}
