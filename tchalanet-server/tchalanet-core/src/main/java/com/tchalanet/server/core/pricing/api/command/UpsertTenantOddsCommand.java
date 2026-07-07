package com.tchalanet.server.core.pricing.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.pricing.api.model.PricingVariantCode;
import com.tchalanet.server.core.pricing.api.model.TenantPricingOddsView;
import java.math.BigDecimal;

public record UpsertTenantOddsCommand(
    TenantId tenantId,
    String gameCode,
    PricingVariantCode pricingVariantCode,
    String betType,
    Short betOption,
    BigDecimal odds,
    UserId actorId
) implements Command<TenantPricingOddsView> {}
