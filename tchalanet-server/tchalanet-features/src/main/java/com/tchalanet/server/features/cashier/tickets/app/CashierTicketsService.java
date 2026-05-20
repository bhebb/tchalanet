package com.tchalanet.server.features.cashier.tickets.app;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.sales.api.command.cancel.CancelTicketCommand;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketCommand;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketLineInput;
import com.tchalanet.server.core.sales.api.model.communication.SaleCommunicationOptions;
import com.tchalanet.server.core.sales.api.query.receipt.FormatTicketReceiptMessageQuery;
import com.tchalanet.server.core.sales.api.query.preview.PreviewTicketSaleQuery;
import com.tchalanet.server.features.cashier.operationalcontext.ResolveSellerOperationalContextRequest;
import com.tchalanet.server.features.cashier.operationalcontext.SellerOperation;
import com.tchalanet.server.features.cashier.operationalcontext.SellerOperationalContextResolver;
import com.tchalanet.server.features.cashier.tickets.model.CashierTicketCancelRequest;
import com.tchalanet.server.features.cashier.tickets.model.CashierTicketCancelResponse;
import com.tchalanet.server.features.cashier.tickets.model.CashierTicketLineRequest;
import com.tchalanet.server.features.cashier.tickets.model.CashierTicketPreviewRequest;
import com.tchalanet.server.features.cashier.tickets.model.CashierTicketPreviewResponse;
import com.tchalanet.server.features.cashier.tickets.model.CashierTicketSellRequest;
import com.tchalanet.server.features.cashier.tickets.model.CashierTicketSellResponse;
import com.tchalanet.server.features.cashier.tickets.model.SendTicketReceiptRequest;
import com.tchalanet.server.features.cashier.tickets.model.SendTicketReceiptResponse;
import com.tchalanet.server.platform.communication.api.CommunicationApi;
import com.tchalanet.server.platform.communication.api.model.request.SendOutboundMessageRequest;
import com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel;
import com.tchalanet.server.platform.communication.api.model.value.OutboundRecipient;
import java.util.LinkedHashMap;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CashierTicketsService {

    private final QueryBus queryBus;
    private final CommandBus commandBus;
    private final SellerOperationalContextResolver sellerContextResolver;
    private final CommunicationApi communicationApi;

    public CashierTicketPreviewResponse preview(
        TchRequestContext ctx,
        CashierTicketPreviewRequest request
    ) {
        validateSellerContext(ctx, request.terminalId());
        var result = queryBus.ask(new PreviewTicketSaleQuery(
            DrawId.of(request.drawId()),
            drawChannelId(request.drawChannelId()),
            CurrencyCode.of(request.currency()),
            lines(request.lines())
        ));
        return new CashierTicketPreviewResponse(
            result.decision(),
            result.issues(),
            result.actionAvailability(),
            result.sellerInstruction(),
            result.warning()
        );
    }

    public CashierTicketSellResponse sell(TchRequestContext ctx, CashierTicketSellRequest request) {
        validateSellerContext(ctx, request.terminalId());
        var result = commandBus.execute(new SellTicketCommand(
            DrawId.of(request.drawId()),
            drawChannelId(request.drawChannelId()),
            CurrencyCode.of(request.currency()),
            lines(request.lines()),
            SaleCommunicationOptions.none()
        ));
        return new CashierTicketSellResponse(
            result.outcome(),
            result.ticketId(),
            result.ticketCode(),
            result.publicCode(),
            result.saleStatus(),
            result.issues(),
            result.backup(),
            result.actionAvailability(),
            result.sellerInstruction()
        );
    }

    public CashierTicketCancelResponse cancel(
        TchRequestContext ctx,
        TicketId ticketId,
        CashierTicketCancelRequest request
    ) {
        validateSellerContext(ctx, request.terminalId());
        var result = commandBus.execute(new CancelTicketCommand(ticketId, request.reason()));
        return new CashierTicketCancelResponse(
            result.ticketId(),
            result.outcome(),
            result.cancelledAt(),
            result.issues()
        );
    }

    public SendTicketReceiptResponse send(
        TchRequestContext ctx,
        TicketId ticketId,
        SendTicketReceiptRequest request
    ) {
        validateSellerContext(ctx, request.terminalId());
        validateRecipient(request);
        var message = queryBus.ask(new FormatTicketReceiptMessageQuery(ticketId, request.locale()));
        var recipient = recipient(ctx, request);
        var metadata = new LinkedHashMap<String, Object>();
        metadata.put("templateKey", "ticket.receipt.text.v1");
        metadata.put("ticketId", ticketId.value().toString());
        metadata.put("publicCode", message.metadata().get("publicCode"));
        metadata.put("channel", request.channel().name());
        metadata.put("recipient", recipientValue(request));
        metadata.put("dedupKey", dedupKey(ticketId, request.channel(), recipientValue(request)));
        metadata.put("correlationKey", dedupKey(ticketId, request.channel(), recipientValue(request)));
        metadata.put("idempotencyKey", dedupKey(ticketId, request.channel(), recipientValue(request)));
        metadata.put("subject", message.subject());
        metadata.put("body", message.body());
        metadata.put("message", message.body());

        communicationApi.enqueue(new SendOutboundMessageRequest(
            "TICKET_RECEIPT",
            request.channel(),
            recipient,
            message.locale(),
            metadata
        ));

        return new SendTicketReceiptResponse(
            ticketId,
            request.channel(),
            recipientValue(request),
            true,
            false
        );
    }

    private void validateSellerContext(TchRequestContext ctx, java.util.UUID terminalId) {
        sellerContextResolver.resolve(new ResolveSellerOperationalContextRequest(
            ctx,
            TerminalId.of(terminalId),
            SellerOperation.SELL
        ));
    }

    private DrawChannelId drawChannelId(java.util.UUID value) {
        return value == null ? null : DrawChannelId.of(value);
    }

    private OutboundRecipient recipient(TchRequestContext ctx, SendTicketReceiptRequest request) {
        return switch (request.channel()) {
            case SLACK, SLACK_INTERNAL, SLACK_TENANT_WEBHOOK ->
                new OutboundRecipient(ctx.effectiveTenantIdOrNull(), ctx.userId(), null, request.channelKey());
            case EMAIL, SMS, WHATSAPP, PUSH ->
                new OutboundRecipient(ctx.effectiveTenantIdOrNull(), ctx.userId(), request.to(), null);
        };
    }

    private void validateRecipient(SendTicketReceiptRequest request) {
        if (request.channel() == null) {
            throw ProblemRest.badRequest("channel.required");
        }
        var recipient = recipientValue(request);
        if (recipient == null || recipient.isBlank()) {
            throw ProblemRest.badRequest("recipient.required");
        }
    }

    private String recipientValue(SendTicketReceiptRequest request) {
        return switch (request.channel()) {
            case SLACK, SLACK_INTERNAL, SLACK_TENANT_WEBHOOK -> request.channelKey();
            case EMAIL, SMS, WHATSAPP, PUSH -> request.to();
        };
    }

    private String dedupKey(TicketId ticketId, CommunicationChannel channel, String recipient) {
        return ticketId.value() + ":" + channel.name() + ":" + recipient;
    }

    private List<SellTicketLineInput> lines(List<CashierTicketLineRequest> lines) {
        return java.util.stream.IntStream.range(0, lines.size())
            .mapToObj(index -> line(index, lines.get(index)))
            .toList();
    }

    private SellTicketLineInput line(int index, CashierTicketLineRequest line) {
        return new SellTicketLineInput(
            index + 1,
            line.gameCode(),
            line.betType(),
            line.selection(),
            line.betOption(),
            line.stake()
        );
    }
}
