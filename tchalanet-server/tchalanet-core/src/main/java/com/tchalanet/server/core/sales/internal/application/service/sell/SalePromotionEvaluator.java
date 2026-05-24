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
import com.tchalanet.server.core.sales.internal.application.service.sell.model.SaleAgentPromotionContext;
import com.tchalanet.server.core.session.api.model.ValidatedPosOperationContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class SalePromotionEvaluator {

    private final QueryBus queryBus;

    public PromotionDecision evaluate(
        TenantId tenantId,
        SellTicketCommand command,
        ValidatedPosOperationContext pos,
        SaleAgentPromotionContext agentCtx,
        Instant now,
        Money paidTotal,
        SaleEvaluationMode mode
    ) {
        var paidGameCodes = command.lines().stream()
            .map(l -> l.gameCode().name())
            .distinct()
            .toList();

        var phase = switch (mode) {
            case PREVIEW -> PromotionEvaluationPhase.SALE_PREVIEW;
            case FINAL -> PromotionEvaluationPhase.SALE_CONFIRMATION;
        };

        var context = new PromotionEvaluationContext(
            tenantId,
            phase,
            now,
            agentCtx.agentId(),
            agentCtx.agentPath(),
            agentCtx.zoneId(),
            agentCtx.zonePath(),
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
