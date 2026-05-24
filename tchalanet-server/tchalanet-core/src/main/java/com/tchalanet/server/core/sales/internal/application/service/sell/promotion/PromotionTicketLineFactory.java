package com.tchalanet.server.core.sales.internal.application.service.sell.promotion;

import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.catalog.pricing.api.PricingCatalog;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.TicketLineId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.promotion.api.model.PromotionDecision;
import com.tchalanet.server.core.promotion.api.model.PromotionEffect;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketLine;
import com.tchalanet.server.core.selection.api.SelectionApi;

import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PromotionTicketLineFactory {

    private final IdGenerator idGenerator;
    private final SelectionApi selectionApi;
    private final PricingCatalog pricingCatalog;
    private final PromotionSelectionResolver selectionResolver;

    public List<TicketLine> createLines(
        PromotionEffect effect,
        PromotionDecision decision,
        SellTicketCommand command,
        CurrencyCode currency
    ) {
        if (effect.type() != PromotionEffectType.FREE_GAME_LINE
            && effect.type() != PromotionEffectType.FREE_EXTRA_LINES) {
            return List.of();
        }

        var out = new ArrayList<TicketLine>();

        for (int i = 0; i < effect.quantity(); i++) {
            out.add(createLine(effect, decision, command, currency, i));
        }

        return List.copyOf(out);
    }

    private TicketLine createLine(
        PromotionEffect effect,
        PromotionDecision decision,
        SellTicketCommand command,
        CurrencyCode currency,
        int index
    ) {
        var gameCode = GameCode.valueOf(effect.gameCode());
        var betType = resolveBetTypeForPromoGame(gameCode);
        var betOption = resolveBetOptionForPromoGame(gameCode);

        var selectionResult = selectionResolver.resolveSelection(
            decision,
            effect,
            command,
            index,
            betType,
            betOption
        );

        var payoutBase = effect.amount().setScale(2, RoundingMode.UNNECESSARY);

        var odds = pricingCatalog
            .oddsFor(command.tenantId(), gameCode.name(), betType, betOption)
            .setScale(4, RoundingMode.HALF_UP);

        var potential = payoutBase.multiply(odds).setScale(2, RoundingMode.HALF_UP);

        return TicketLine.promotionLine(
            TicketLineId.of(idGenerator.newUuid()),
            nextPromotionLineNumber(command, index),
            gameCode,
            betType,
            selectionApi.canonicalize(betType, betOption, selectionResult.rawSelection()),
            Money.zero(currency),
            new Money(payoutBase, currency),
            odds,
            new Money(potential, currency),
            betOption,
            selectionResult.source(),
            decision.decisionId()
        );
    }
}
