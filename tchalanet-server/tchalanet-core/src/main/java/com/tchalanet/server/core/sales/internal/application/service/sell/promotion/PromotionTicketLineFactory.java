package com.tchalanet.server.core.sales.internal.application.service.sell.promotion;

import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.catalog.pricing.api.PricingCatalog;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.TicketLineId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.promotion.api.model.PromotionDecision;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffect;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffectType;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketCommand;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptI18nKeys;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketLine;
import com.tchalanet.server.core.selection.api.SelectionApi;
import com.tchalanet.server.catalog.game.api.model.BetType;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
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
        List<TicketLine> existingLines,
        SellTicketCommand command,
        CurrencyCode currency
    ) {
        if (effect.type() != PromotionEffectType.FREE_GAME_LINE) {
            return List.of();
        }

        var baseLineNumber = existingLines == null ? 0
            : existingLines.stream().mapToInt(TicketLine::lineNumber).max().orElse(0);

        var out = new ArrayList<TicketLine>();

        for (int i = 0; i < effect.quantity(); i++) {
            out.add(createLine(effect, decision, command, currency, baseLineNumber + i + 1, i));
        }

        return List.copyOf(out);
    }

    private TicketLine createLine(
        PromotionEffect effect,
        PromotionDecision decision,
        SellTicketCommand command,
        CurrencyCode currency,
        int lineNumber,
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
            lineNumber,
            gameCode,
            betType,
            selectionApi.canonicalize(betType, betOption, selectionResult.rawSelection()),
            Money.zero(currency),
            new Money(payoutBase, currency),
            odds,
            new Money(potential, currency),
            betOption,
            com.tchalanet.server.core.sales.api.model.promotion.TicketLineSelectionSource.valueOf(String.valueOf(selectionResult.source())),
            decision.decisionId(),
            promotionLabel(effect),
            effect.type().name()
        );
    }

    private String promotionLabel(PromotionEffect effect) {
        if (effect.reason() != null && !effect.reason().isBlank()) {
            return effect.reason().trim();
        }
        return TicketReceiptI18nKeys.PROMOTION_FREE_GAME_LINE;
    }

    private BetType resolveBetTypeForPromoGame(GameCode gameCode) {
        // Use the game's first allowed bet type (EnumSet preserves declaration order).
        // TODO: add betType field to PromotionEffect if multi-bet-type games need explicit selection.
        return gameCode.allowedBetTypes().iterator().next();
    }

    private Short resolveBetOptionForPromoGame(GameCode gameCode) {
        return null;
    }

}
