package com.tchalanet.server.core.pricing.internal.application.query;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.pricing.api.model.OddsSource;
import com.tchalanet.server.core.pricing.api.model.SellerTerminalPayoutRuleResolutionView;
import com.tchalanet.server.core.pricing.api.query.ResolveSellerTerminalPayoutRuleQuery;
import com.tchalanet.server.core.pricing.internal.application.port.out.SellerTerminalOddsOverrideReaderPort;
import com.tchalanet.server.core.pricing.internal.application.port.out.TenantPricingOddsReaderPort;
import com.tchalanet.server.core.pricing.internal.domain.TenantPricingOdds;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ResolveSellerTerminalPayoutRuleQueryHandler
    implements QueryHandler<ResolveSellerTerminalPayoutRuleQuery, SellerTerminalPayoutRuleResolutionView> {

    private final SellerTerminalOddsOverrideReaderPort overrideReader;
    private final TenantPricingOddsReaderPort tenantPricingReader;

    @Override
    public SellerTerminalPayoutRuleResolutionView handle(ResolveSellerTerminalPayoutRuleQuery q) {
        var tenantDefault = tenantPricingReader
            .findByNaturalKey(
                q.tenantId(),
                TenantPricingOdds.normalizeGameCode(q.gameCode()),
                q.pricingVariantCode())
            .orElseThrow(() -> new IllegalStateException(
                "tenant default pricing rule missing for tenant=%s game=%s variant=%s"
                    .formatted(q.tenantId(), q.gameCode(), q.pricingVariantCode())));

        var override = overrideReader.findActiveByNaturalKey(
            q.tenantId(),
            q.sellerTerminalId(),
            TenantPricingOdds.normalizeGameCode(q.gameCode()),
            q.pricingVariantCode());

        if (override.isPresent()) {
            var sellerOverride = override.get();
            if (sellerOverride.payoutRuleType() != tenantDefault.payoutRuleType()) {
                throw new IllegalStateException(
                    "seller terminal pricing override cannot change payout rule type for game=%s variant=%s"
                        .formatted(q.gameCode(), q.pricingVariantCode()));
            }
            return new SellerTerminalPayoutRuleResolutionView(
                q.gameCode(),
                q.pricingVariantCode(),
                q.betType(),
                q.betOption(),
                tenantDefault.payoutRuleType(),
                tenantDefault.odds(),
                tenantDefault.fixedAmount(),
                sellerOverride.payoutRuleType(),
                sellerOverride.odds(),
                sellerOverride.fixedAmount(),
                sellerOverride.payoutRuleType(),
                sellerOverride.odds(),
                sellerOverride.fixedAmount(),
                OddsSource.SELLER_TERMINAL_OVERRIDE
            );
        }

        return new SellerTerminalPayoutRuleResolutionView(
            q.gameCode(),
            q.pricingVariantCode(),
            q.betType(),
            q.betOption(),
            tenantDefault.payoutRuleType(),
            tenantDefault.odds(),
            tenantDefault.fixedAmount(),
            null,
            null,
            null,
            tenantDefault.payoutRuleType(),
            tenantDefault.odds(),
            tenantDefault.fixedAmount(),
            OddsSource.TENANT_DEFAULT
        );
    }
}
