package com.tchalanet.server.core.sales.internal.application.service.sell.promotion;

import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.core.promotion.api.model.PromotionDecision;
import com.tchalanet.server.core.promotion.api.model.PromotionDecisionStatus;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketCommand;
import com.tchalanet.server.core.sales.api.model.money.TicketCharge;
import com.tchalanet.server.core.sales.internal.application.service.sell.model.AppliedSalePromotionEffects;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketLine;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Applies PromotionDecision effects to sale materialization.
 * V1 creates/updates TicketLine snapshots. Settlement must later consume only these snapshots.
 */
@Component
@RequiredArgsConstructor
public class SalePromotionEffectApplier {

    private final PromotionTicketLineFactory promotionTicketLineFactory;
    private final PromotionOddsBoostApplier promotionOddsBoostApplier;
    private final PromotionChargeApplier promotionChargeApplier;

    public AppliedSalePromotionEffects apply(
        PromotionDecision decision,
        List<TicketLine> paidLines,
        List<TicketCharge> charges,
        SellTicketCommand command,
        SellerTerminalId sellerTerminalId,
        CurrencyCode currency
    ) {
        if (decision == null || decision.status() != PromotionDecisionStatus.APPLIED) {
            return AppliedSalePromotionEffects.none(paidLines, charges);
        }

        var lines = new ArrayList<>(paidLines);
        var finalCharges = new ArrayList<>(charges);

        for (var effect : decision.effects()) {
            switch (effect.type()) {
                case WAIVE_CHARGE -> promotionChargeApplier.apply(finalCharges, effect, decision);
                case BOOST_ODDS -> promotionOddsBoostApplier.apply(lines, effect, decision, currency);
                case FREE_GAME_LINE ->
                    lines.addAll(promotionTicketLineFactory.createLines(
                        effect, decision, lines, command, sellerTerminalId, currency));
            }
        }

        return new AppliedSalePromotionEffects(
            List.copyOf(lines),
            List.copyOf(finalCharges)
        );
    }
}
