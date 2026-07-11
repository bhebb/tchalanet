package com.tchalanet.server.core.sales.internal.application.service.sell.promotion;

import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TicketLineId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.promotion.api.model.PromotionDecision;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffect;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffectType;
import com.tchalanet.server.core.pricing.api.model.OddsSource;
import com.tchalanet.server.core.pricing.api.model.PayoutRuleType;
import com.tchalanet.server.core.pricing.api.query.ResolveSellerTerminalPayoutRuleQuery;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketCommand;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineSelectionSource;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptI18nKeys;
import com.tchalanet.server.core.sales.api.model.settlement.PayoutRuleSnapshot;
import com.tchalanet.server.core.sales.api.model.settlement.SettlementRuleCode;
import com.tchalanet.server.core.sales.api.model.settlement.SettlementTermSnapshot;
import com.tchalanet.server.core.sales.api.model.settlement.SettlementTermSource;
import com.tchalanet.server.core.sales.api.model.settlement.SettlementTermsSnapshot;
import com.tchalanet.server.core.sales.api.model.settlement.SettlementWinMode;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketLine;
import com.tchalanet.server.core.sales.internal.domain.service.result.SettlementVariantResolver;
import com.tchalanet.server.core.selection.api.SelectionApi;
import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.platform.tenantgame.api.model.SelectionPolicy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PromotionTicketLineFactory {

    private final IdGenerator idGenerator;
    private final SelectionApi selectionApi;
    private final QueryBus queryBus;
    private final PromotionSelectionResolver selectionResolver;
    private static final int MAX_SELECTION_ATTEMPTS = 50;

    public List<TicketLine> createLines(
        PromotionEffect effect,
        PromotionDecision decision,
        List<TicketLine> existingLines,
        SellTicketCommand command,
        SellerTerminalId sellerTerminalId,
        CurrencyCode currency
    ) {
        if (effect.type() != PromotionEffectType.FREE_GAME_LINE) {
            return List.of();
        }

        var baseLineNumber = existingLines == null ? 0
            : existingLines.stream().mapToInt(TicketLine::lineNumber).max().orElse(0);

        var out = new ArrayList<TicketLine>();

        for (int i = 0; i < effect.quantity(); i++) {
            out.add(createLine(
                effect,
                decision,
                existingLines == null ? List.of() : existingLines,
                out,
                command,
                sellerTerminalId,
                currency,
                baseLineNumber + i + 1,
                i));
        }

        return List.copyOf(out);
    }

    private TicketLine createLine(
        PromotionEffect effect,
        PromotionDecision decision,
        List<TicketLine> existingLines,
        List<TicketLine> generatedLines,
        SellTicketCommand command,
        SellerTerminalId sellerTerminalId,
        CurrencyCode currency,
        int lineNumber,
        int index
    ) {
        var gameCode = GameCode.valueOf(effect.gameCode());
        var betType = resolveBetTypeForPromoGame(gameCode);
        var betOption = resolveBetOptionForPromoGame(gameCode);

        var selectionResult = resolveUniqueSelection(
            decision,
            effect,
            existingLines,
            generatedLines,
            command,
            index,
            betType,
            betOption
        );

        var payoutBase = effect.amount().setScale(2, RoundingMode.UNNECESSARY);

        var pricingVariantCode = SettlementVariantResolver.resolve(
            betType,
            betOption,
            selectionResult.rawSelection());

        var payoutRuleResolution = queryBus.ask(new ResolveSellerTerminalPayoutRuleQuery(
            command.tenantId(),
            sellerTerminalId,
            gameCode.name(),
            pricingVariantCode,
            betType.name(),
            betOption));

        Objects.requireNonNull(payoutRuleResolution, "pricing payout rule resolution is required");
        var payoutRule = payoutRuleResolution.effectiveRuleType() == PayoutRuleType.FIXED_AMOUNT
            ? PayoutRuleSnapshot.fixedAmount(payoutRuleResolution.effectiveFixedAmount())
            : PayoutRuleSnapshot.stakeMultiplier(payoutRuleResolution.effectiveMultiplier());
        var odds = compatibilityOdds(payoutRule).setScale(4, RoundingMode.HALF_UP);
        var settlementTermsSnapshot = SettlementTermsSnapshot.current(
            SelectionPolicy.EXPLICIT_ONLY,
            List.of(new SettlementTermSnapshot(
                SettlementRuleCode.fromPricingVariant(pricingVariantCode),
                betOption,
                null,
                payoutRule,
                payoutRule.type() == PayoutRuleType.STAKE_MULTIPLIER ? payoutBase : null,
                SettlementWinMode.ALTERNATIVE,
                toSettlementTermSource(payoutRuleResolution.source()))));

        return TicketLine.promotionLine(
            TicketLineId.of(idGenerator.newUuid()),
            lineNumber,
            gameCode,
            betType,
            selectionApi.canonicalize(betType, betOption, selectionResult.rawSelection()),
            Money.zero(currency),
            new Money(payoutBase, currency),
            odds,
            settlementTermsSnapshot,
            betOption,
            selectionResult.source(),
            decision.decisionId(),
            promotionLabel(effect),
            effect.type().name()
        );
    }

    private BigDecimal compatibilityOdds(PayoutRuleSnapshot payoutRule) {
        return payoutRule.type() == PayoutRuleType.STAKE_MULTIPLIER
            ? payoutRule.multiplier()
            : BigDecimal.ONE;
    }

    private SettlementTermSource toSettlementTermSource(OddsSource source) {
        return source == OddsSource.SELLER_TERMINAL_OVERRIDE
            ? SettlementTermSource.SELLER_TERMINAL_OVERRIDE
            : SettlementTermSource.TENANT_DEFAULT;
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
        if (gameCode == GameCode.HT_MARYAJ_GRATIS) {
            return 2;
        }
        return null;
    }

    private PromotionSelectionResolver.SelectionResult resolveUniqueSelection(
        PromotionDecision decision,
        PromotionEffect effect,
        List<TicketLine> existingLines,
        List<TicketLine> generatedLines,
        SellTicketCommand command,
        int index,
        BetType betType,
        Short betOption
    ) {
        var usedSelections = usedSelections(betType, existingLines, generatedLines);
        for (int attempt = 0; attempt < MAX_SELECTION_ATTEMPTS; attempt++) {
            var result = selectionResolver.resolveSelection(
                decision,
                effect,
                command,
                index,
                betType,
                betOption
            );
            var selectionKey = selectionApi.canonicalize(betType, betOption, result.rawSelection()).key().value();
            if (!usedSelections.contains(selectionKey)) {
                return result;
            }
            if (result.source() != TicketLineSelectionSource.PROMOTION_GENERATED) {
                throw new IllegalArgumentException("promotion.free_game_selection_duplicate");
            }
        }
        throw new IllegalArgumentException("promotion.free_game_selection_duplicate_exhausted");
    }

    private Set<String> usedSelections(
        BetType betType,
        List<TicketLine> existingLines,
        List<TicketLine> generatedLines
    ) {
        var used = new HashSet<String>();
        collectSelections(used, betType, existingLines);
        collectSelections(used, betType, generatedLines);
        return used;
    }

    private void collectSelections(Set<String> used, BetType betType, List<TicketLine> lines) {
        if (lines == null) return;
        lines.stream()
            .filter(line -> line != null && line.betType() == betType && line.selection() != null)
            .map(line -> line.selection().key().value())
            .forEach(used::add);
    }
}
