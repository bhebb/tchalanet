package com.tchalanet.server.core.pricing.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.pricing.api.model.PayoutRuleType;
import com.tchalanet.server.core.pricing.api.model.PricingVariantCode;

import java.math.BigDecimal;
import java.time.Instant;

public record UpsertSellerTerminalPricingRuleOverrideCommand(
    TenantId tenantId,
    SellerTerminalId sellerTerminalId,
    String gameCode,
    PricingVariantCode pricingVariantCode,
    String betType,
    Short betOption,
    BigDecimal odds,
    PayoutRuleType payoutRuleType,
    BigDecimal fixedAmount,
    Instant effectiveFrom,
    Instant effectiveTo,
    String reason,
    UserId actorId
) implements Command<UpsertSellerTerminalPricingRuleOverrideResult> {

    public UpsertSellerTerminalPricingRuleOverrideCommand(
        TenantId tenantId,
        SellerTerminalId sellerTerminalId,
        String gameCode,
        PricingVariantCode pricingVariantCode,
        String betType,
        Short betOption,
        BigDecimal odds,
        Instant effectiveFrom,
        Instant effectiveTo,
        String reason,
        UserId actorId
    ) {
        this(
            tenantId,
            sellerTerminalId,
            gameCode,
            pricingVariantCode,
            betType,
            betOption,
            odds,
            null,
            null,
            effectiveFrom,
            effectiveTo,
            reason,
            actorId
        );
    }
}
