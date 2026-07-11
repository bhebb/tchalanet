package com.tchalanet.server.core.pricing.api.command;

import com.tchalanet.server.common.types.id.SellerTerminalOddsOverrideId;

public record UpsertSellerTerminalPricingRuleOverrideResult(
    SellerTerminalOddsOverrideId id,
    boolean created
) {}
