package com.tchalanet.server.core.sales.internal.application.service.result;

import com.tchalanet.server.catalog.game.api.model.BetOption;
import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.core.sales.api.settlement.ComputedSettlementVariantView;
import com.tchalanet.server.core.sales.api.settlement.SettlementExplanationApi;
import com.tchalanet.server.core.sales.internal.domain.model.result.SettlementVariant;
import com.tchalanet.server.core.sales.internal.domain.service.result.SettlementVariantResolver;
import org.springframework.stereotype.Component;

/**
 * Recomputes the settlement variant of a sale line from {@code betType + betOption + selection}
 * and exposes commercial/admin labels. No persistence, no bus — pure delegation to the domain
 * resolver plus catalog labels.
 */
@Component
public class SettlementExplanationService implements SettlementExplanationApi {

    @Override
    public ComputedSettlementVariantView explain(BetType betType, Short betOption, String selection) {
        var variant = SettlementVariantResolver.resolve(betType, betOption, selection);
        return new ComputedSettlementVariantView(
            variant.name(),
            commercialLabel(betType, betOption, variant),
            variant.adminLabel()
        );
    }

    private static String commercialLabel(BetType betType, Short betOption, SettlementVariant variant) {
        if (betType.requiresOption()) {
            return BetOption.from(betType, betOption).label();
        }
        // Option-less bet types (Bòlèt): commercial and admin labels are the same.
        return variant.adminLabel();
    }
}
