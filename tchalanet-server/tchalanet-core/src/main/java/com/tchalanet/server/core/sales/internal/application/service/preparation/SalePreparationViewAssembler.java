package com.tchalanet.server.core.sales.internal.application.service.preparation;

import com.tchalanet.server.core.sales.api.model.preparation.SalePreparationLineView;
import com.tchalanet.server.core.sales.api.model.preparation.SalePreparationPromotionLineView;
import com.tchalanet.server.core.sales.api.model.preparation.SalePreparationView;
import com.tchalanet.server.core.sales.internal.application.service.sell.model.PreparedSale;
import com.tchalanet.server.core.sales.internal.domain.model.preparation.SalePreparation;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SalePreparationViewAssembler {

    /** Full view at prepare time, with the freshly computed sale lines. */
    public SalePreparationView toView(SalePreparation preparation, PreparedSale prepared) {
        var lines = prepared.ticketLines().stream()
            .map(line -> new SalePreparationLineView(
                line.lineNumber(),
                line.gameCode().name(),
                line.betType().name(),
                line.betOption(),
                line.selection().key().value(),
                line.stakeAmount().amount(),
                line.oddsSnapshot(),
                line.potentialPayoutAmount().amount(),
                line.origin().name()))
            .toList();
        return new SalePreparationView(
            preparation.id(),
            preparation.status(),
            preparation.expiresAt(),
            prepared.moneyBreakdown().total().currency().value(),
            prepared.moneyBreakdown().total().amount(),
            lines,
            promotionLines(preparation),
            prepared.notices());
    }

    /**
     * Lightweight view after a mutation on a stored preparation (regenerate):
     * paid lines are unchanged client-side and are not re-derived here.
     */
    public SalePreparationView toView(SalePreparation preparation) {
        return new SalePreparationView(
            preparation.id(),
            preparation.status(),
            preparation.expiresAt(),
            null,
            null,
            List.of(),
            promotionLines(preparation),
            List.of());
    }

    private List<SalePreparationPromotionLineView> promotionLines(SalePreparation preparation) {
        return preparation.promotionLines().stream()
            .map(l -> new SalePreparationPromotionLineView(
                l.lineRef(),
                l.gameCode(),
                l.betType(),
                l.betOption(),
                l.selection(),
                l.payoutBaseAmount() == null ? BigDecimal.ZERO : l.payoutBaseAmount(),
                l.regenerable(),
                Math.max(0, l.maxRegenerations() - l.regenerationCount())))
            .toList();
    }
}
