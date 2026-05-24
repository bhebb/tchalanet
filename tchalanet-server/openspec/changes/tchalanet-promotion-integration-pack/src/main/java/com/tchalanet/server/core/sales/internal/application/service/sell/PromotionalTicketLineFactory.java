package com.tchalanet.server.core.sales.internal.application.service.sell;

import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.TicketLineId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.promotion.api.model.PromotionDecision;
import com.tchalanet.server.core.promotion.api.model.PromotionEffect;
import com.tchalanet.server.core.sales.api.model.promotion.*;
import com.tchalanet.server.core.sales.api.model.status.TicketLineResultStatus;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketLine;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketLinePromotionFields;
import com.tchalanet.server.core.selection.api.SelectionApi;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PromotionalTicketLineFactory {
    private final IdGenerator idGenerator;
    private final SelectionApi selectionApi;
    private final PromotionalSelectionGenerator selectionGenerator;
    private final PromotionalPricingResolver pricingResolver;

    public TicketLine create(PromotionDecision decision, PromotionEffect effect, int lineNumber, CurrencyCode currency) {
        var gameCode = GameCode.valueOf(effect.gameCode());
        var rawSelection = selectionGenerator.generate(effect);
        var pricing = pricingResolver.resolve(effect, currency);
        var payoutBase = new Money(effect.amount(), currency);
        var potential = payoutBase.amount().multiply(pricing.odds()).setScale(2, RoundingMode.HALF_UP);

        return new TicketLine(
            TicketLineId.of(idGenerator.newUuid()),
            lineNumber,
            gameCode,
            pricing.betType(),
            selectionApi.canonicalize(pricing.betType(), null, rawSelection),
            Money.zero(currency),
            payoutBase,
            pricing.odds(),
            new Money(potential, currency),
            null,
            TicketLineResultStatus.PENDING,
            Money.zero(currency),
            new TicketLinePromotionFields(
                TicketLineOrigin.PROMOTION,
                TicketLinePricingSource.PROMOTION,
                TicketLineSelectionSource.AUTO_GENERATED,
                decision.decisionId()
            )
        );
    }
}
