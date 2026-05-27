package com.tchalanet.server.features.cashier.tickets.app;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.sales.api.command.print.RecordTicketPrintCommand;
import com.tchalanet.server.core.sales.api.model.print.PrintOutputFormat;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptPrintContent;
import com.tchalanet.server.core.sales.api.query.receipt.FormatTicketReceiptMessageQuery;
import com.tchalanet.server.core.sales.api.query.receipt.FormatTicketReceiptPrintQuery;
import com.tchalanet.server.features.cashier.operationalcontext.ResolveSellerOperationalContextRequest;
import com.tchalanet.server.features.cashier.operationalcontext.SellerOperation;
import com.tchalanet.server.features.cashier.operationalcontext.SellerOperationalContextResolver;
import com.tchalanet.server.features.cashier.operationalcontext.SellerOperationalContextView;
import com.tchalanet.server.features.cashier.tickets.mapper.TicketPrintCommunicationMapper;
import com.tchalanet.server.features.cashier.tickets.mapper.TicketPrintDocumentMapper;
import com.tchalanet.server.features.cashier.tickets.model.PrintTicketRequest;
import com.tchalanet.server.features.cashier.tickets.model.SendTicketReceiptRequest;
import com.tchalanet.server.features.cashier.tickets.model.SendTicketReceiptResponse;
import com.tchalanet.server.platform.communication.api.CommunicationApi;
import com.tchalanet.server.platform.communication.api.model.request.SendOutboundMessageRequest;
import com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel;
import com.tchalanet.server.platform.communication.api.model.value.OutboundRecipient;
import com.tchalanet.server.platform.document.api.DocumentApi;
import com.tchalanet.server.platform.document.api.model.DocumentFormat;
import com.tchalanet.server.platform.document.api.model.RenderedDocument;
import com.tchalanet.server.platform.tenantconfig.api.TenantConfigApi;
import com.tchalanet.server.platform.tenantconfig.api.model.request.GetTenantByIdRequest;
import com.tchalanet.server.platform.tenantconfig.api.model.view.TenantInternalDocumentConfig;
import java.util.LinkedHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * Cashier-facing receipt operations: render printable bytes (PDF/ESC_POS) and dispatch receipts via
 * {@link com.tchalanet.server.platform.communication.api.CommunicationApi}.
 */
@Service
@RequiredArgsConstructor
public class CashierTicketReceiptService {

    private final QueryBus queryBus;
    private final CommandBus commandBus;
    private final DocumentApi documentApi;
    private final CommunicationApi communicationApi;
    private final TenantConfigApi tenantConfigApi;
    private final TicketPrintDocumentMapper documentMapper;
    private final TicketPrintCommunicationMapper communicationMapper;
    private final SellerOperationalContextResolver sellerContextResolver;

    public ResponseEntity<ByteArrayResource> print(
        TchRequestContext ctx,
        TicketId ticketId,
        PrintTicketRequest request
    ) {
        var sellerContext = validateSellerContextForPrint(ctx, request);

        var receipt = queryBus.ask(new FormatTicketReceiptPrintQuery(ticketId, request.format(), request.buyerLocale()));
        var receiptConfig = resolveReceiptConfig(ctx);
        var rendered = documentApi.render(
            documentMapper.toRenderRequest(receipt, request, receiptConfig)
        );

        if (request.recordPrint()) {
            commandBus.execute(new RecordTicketPrintCommand(
                ticketId,
                request.format(),
                request.reprintReason(),
                sellerContext.actorUserId(),
                sellerContext.terminalId(),
                sellerContext.outletId(),
                sellerContext.salesSessionId(),
                ctx.correlationId()
            ));
        }

        if (request.shouldSendToBuyer()) {
            var outboundDocument = toCommunicationDocument(receipt, request, rendered, receiptConfig);
            var message = queryBus.ask(new FormatTicketReceiptMessageQuery(ticketId, request.buyerLocale()));
            communicationMapper.toOutboundMessages(ctx, receipt, message, outboundDocument, request)
                .forEach(communicationApi::enqueue);
        }

        return toFileResponse(rendered);
    }

    public SendTicketReceiptResponse send(
        TchRequestContext ctx,
        TicketId ticketId,
        SendTicketReceiptRequest request
    ) {
        validateSellerContext(ctx, request.terminalId(), SellerOperation.SEND_RECEIPT);
        validateRecipient(request);
        var message = queryBus.ask(new FormatTicketReceiptMessageQuery(ticketId, request.locale()));
        var recipient = recipient(ctx, request);
        var dedupKey = dedupKey(ticketId, request.channel(), recipientValue(request));

        var metadata = new LinkedHashMap<String, Object>();
        metadata.put("templateKey", "ticket.receipt.text.v1");
        metadata.put("ticketId", ticketId.value().toString());
        metadata.put("publicCode", message.metadata().get("publicCode"));
        metadata.put("channel", request.channel().name());
        metadata.put("recipient", recipientValue(request));
        metadata.put("dedupKey", dedupKey);
        metadata.put("correlationKey", dedupKey);
        metadata.put("idempotencyKey", dedupKey);
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

    // -- print helpers ------------------------------------------------------------

    private TenantInternalDocumentConfig.ReceiptConfig resolveReceiptConfig(TchRequestContext ctx) {
        if (ctx == null || ctx.effectiveTenantIdOrNull() == null) {
            return null;
        }
        var tenantDocument = tenantConfigApi.getTenantDocumentConfig(
            new GetTenantByIdRequest(ctx.effectiveTenantIdRequired())
        );
        return tenantDocument == null ? null : tenantDocument.receipt();
    }

    private ResponseEntity<ByteArrayResource> toFileResponse(RenderedDocument document) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(document.contentType()));
        headers.setCacheControl(CacheControl.noStore());
        headers.setContentDisposition(
            ContentDisposition.inline()
                .filename(document.filename())
                .build()
        );

        return new ResponseEntity<>(
            new ByteArrayResource(document.bytes()),
            headers,
            HttpStatus.OK
        );
    }

    private RenderedDocument toCommunicationDocument(
        TicketReceiptPrintContent receipt,
        PrintTicketRequest request,
        RenderedDocument rendered,
        TenantInternalDocumentConfig.ReceiptConfig receiptConfig
    ) {
        if (rendered.format() == DocumentFormat.PDF) {
            return rendered;
        }

        var pdfRequest = new PrintTicketRequest(
            request.terminalId(),
            PrintOutputFormat.PDF,
            false,
            request.reprintReason(),
            request.deliveryOptions(),
            request.buyerPhoneNumber(),
            request.buyerEmail(),
            request.buyerLocale()
        );

        return documentApi.render(documentMapper.toRenderRequest(receipt, pdfRequest, receiptConfig));
    }

    // -- send helpers -------------------------------------------------------------

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

    // -- operational context ------------------------------------------------------

    private SellerOperationalContextView validateSellerContext(TchRequestContext ctx, java.util.UUID terminalId) {
        return validateSellerContext(ctx, terminalId, SellerOperation.SELL);
    }

    private SellerOperationalContextView validateSellerContext(
        TchRequestContext ctx,
        java.util.UUID terminalId,
        SellerOperation operation
    ) {
        if (terminalId == null) {
            throw ProblemRest.forbidden("cashier.operational_context_required");
        }
        return sellerContextResolver.resolve(new ResolveSellerOperationalContextRequest(
            ctx,
            TerminalId.of(terminalId),
            operation
        ));
    }

    private SellerOperationalContextView validateSellerContextForPrint(TchRequestContext ctx, PrintTicketRequest request) {
        var operation = request.recordPrint() && request.reprintReason() != null && !request.reprintReason().isBlank()
            ? SellerOperation.REPRINT_TICKET
            : SellerOperation.PRINT_TICKET;
        return validateSellerContext(ctx, request.terminalId(), operation);
    }
}
