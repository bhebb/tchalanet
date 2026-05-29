package com.tchalanet.server.core.sales.internal.application.service.sell;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.promotion.api.model.PromotionDecision;
import com.tchalanet.server.core.promotion.api.model.PromotionEvaluationContext;
import com.tchalanet.server.core.promotion.api.model.PromotionEvaluationPhase;
import com.tchalanet.server.core.promotion.api.query.EvaluatePromotionQuery;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketCommand;
import com.tchalanet.server.core.sales.internal.application.sale.SaleEvaluationMode;
import com.tchalanet.server.core.session.api.model.ValidatedPosOperationContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SalePromotionEvaluator {

    private final QueryBus queryBus;

    public PromotionDecision evaluate(
        TenantId tenantId,
        SellTicketCommand command,
        ValidatedPosOperationContext pos,
        Instant now,
        Money paidTotal,
        SaleEvaluationMode mode
    ) {
        var paidGameCodes = command.lines().stream()
            .map(l -> l.gameCode().name())
            .distinct()
            .toList();

        var phase = mode == SaleEvaluationMode.PREVIEW
            ? PromotionEvaluationPhase.SALE_PREVIEW
            : PromotionEvaluationPhase.SALE_CONFIRMATION;

        // Seller context is resolved separately; agent fields are null pending promotion redesign.
        var context = new PromotionEvaluationContext(
            tenantId,
            phase,
            now,
            null,
            List.of(),
            null,
            List.of(),
            pos.outletId(),
            pos.terminalId(),
            pos.salesSessionId(),
            pos.actorUserId(),
            paidTotal.amount(),
            command.currency().code(),
            paidGameCodes,
            false
        );

        return queryBus.ask(new EvaluatePromotionQuery(context));
    }
}
