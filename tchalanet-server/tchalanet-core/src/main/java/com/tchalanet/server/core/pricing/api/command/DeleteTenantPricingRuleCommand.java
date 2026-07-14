package com.tchalanet.server.core.pricing.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.pricing.api.model.PricingVariantCode;

public record DeleteTenantPricingRuleCommand(
    TenantId tenantId, String gameCode, PricingVariantCode pricingVariantCode, UserId actorId)
    implements Command<Void> {}
