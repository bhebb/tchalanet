package com.tchalanet.server.core.sales.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketCommand;
import com.tchalanet.server.core.sales.api.model.communication.SaleCommunicationOptions;
import com.tchalanet.server.core.sales.api.query.preview.PreviewTicketSaleQuery;
import com.tchalanet.server.core.sales.api.query.preview.TicketSalePreviewResult;
import com.tchalanet.server.core.sales.internal.application.sale.SaleAcceptanceEvaluator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
public class PreviewTicketSaleQueryHandler
    implements QueryHandler<PreviewTicketSaleQuery, TicketSalePreviewResult> {

    private final SaleAcceptanceEvaluator evaluator;

    public PreviewTicketSaleQueryHandler(SaleAcceptanceEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public TicketSalePreviewResult handle(PreviewTicketSaleQuery query) {
        var command = new SellTicketCommand(
            query.drawId(),
            query.drawChannelId(),
            query.currency(),
            query.lines(),
            SaleCommunicationOptions.none(),
            java.util.List.of()
        );
        var evaluation = evaluator.evaluatePreview(command, TchContext.currentOrThrow());
        return new TicketSalePreviewResult(
            evaluation.decision(),
            evaluation.issues(),
            evaluation.actionAvailability(),
            evaluation.sellerInstruction(),
            evaluation.warning()
        );
    }
}
