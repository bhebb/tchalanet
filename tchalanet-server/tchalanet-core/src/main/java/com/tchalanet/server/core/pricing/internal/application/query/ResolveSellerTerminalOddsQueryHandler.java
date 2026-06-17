package com.tchalanet.server.core.pricing.internal.application.query;

import com.tchalanet.server.catalog.pricing.api.PricingCatalog;
import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.pricing.api.model.OddsSource;
import com.tchalanet.server.core.pricing.api.model.SellerTerminalOddsResolutionView;
import com.tchalanet.server.core.pricing.api.query.ResolveSellerTerminalOddsQuery;
import com.tchalanet.server.core.pricing.internal.application.port.out.SellerTerminalOddsOverrideReaderPort;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@UseCase
@RequiredArgsConstructor
public class ResolveSellerTerminalOddsQueryHandler
    implements QueryHandler<ResolveSellerTerminalOddsQuery, SellerTerminalOddsResolutionView> {

    private final SellerTerminalOddsOverrideReaderPort reader;
    private final PricingCatalog pricingCatalog;

    @Override
    public SellerTerminalOddsResolutionView handle(ResolveSellerTerminalOddsQuery q) {
        // Tenant default from catalog
        BigDecimal tenantDefault = pricingCatalog.oddsFor(
            q.tenantId(), q.gameCode(),
            // BetType enum from catalog — convert via string
            com.tchalanet.server.catalog.game.api.model.BetType.valueOf(q.betType()),
            q.betOption());

        // Look for active seller_terminal override
        var override = reader.findActiveByNaturalKey(
            q.tenantId(), q.sellerTerminalId(),
            q.gameCode(), q.betType(), q.betOption());

        if (override.isPresent()) {
            var o = override.get();
            return new SellerTerminalOddsResolutionView(
                q.gameCode(), q.betType(), q.betOption(),
                tenantDefault, o.odds(), o.odds(),
                OddsSource.SELLER_TERMINAL_OVERRIDE);
        }

        return new SellerTerminalOddsResolutionView(
            q.gameCode(), q.betType(), q.betOption(),
            tenantDefault, null, tenantDefault,
            OddsSource.TENANT_DEFAULT);
    }
}
