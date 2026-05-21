package com.tchalanet.server.features.cashier.tickets.app;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.sales.api.command.print.RecordTicketPrintCommand;
import com.tchalanet.server.core.sales.api.model.print.PrintOutputFormat;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintView;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptPrintContent;
import com.tchalanet.server.core.sales.api.query.GetTicketPrintViewQuery;
import com.tchalanet.server.core.sales.api.query.receipt.FormatTicketReceiptMessageQuery;
import com.tchalanet.server.core.sales.api.query.receipt.FormatTicketReceiptPrintQuery;
import com.tchalanet.server.features.cashier.operationalcontext.ResolveSellerOperationalContextRequest;
import com.tchalanet.server.features.cashier.operationalcontext.SellerOperation;
import com.tchalanet.server.features.cashier.operationalcontext.SellerOperationalContextResolver;
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
import com.tchalanet.server.platform.document.api.model.DocumentAsset;
import com.tchalanet.server.platform.document.api.model.DocumentFormat;
import com.tchalanet.server.platform.document.api.model.DocumentKind;
import com.tchalanet.server.platform.document.api.model.DocumentLine;
import com.tchalanet.server.platform.document.api.model.DocumentOptions;
import com.tchalanet.server.platform.document.api.model.DocumentRenderRequest;
import com.tchalanet.server.platform.document.api.model.DocumentTemplateKey;
import com.tchalanet.server.platform.document.api.model.RenderedDocument;
import com.tchalanet.server.platform.document.api.model.ReceiptDocumentContent;
import com.tchalanet.server.platform.tenantconfig.api.TenantConfigApi;
import com.tchalanet.server.platform.tenantconfig.api.model.request.GetTenantByIdRequest;
import com.tchalanet.server.platform.tenantconfig.api.model.view.TenantInternalDocumentConfig;
import java.util.LinkedHashMap;
import java.util.List;
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
        validateSellerContextForPrint(ctx, request);
        var printContent = queryBus.ask(new FormatTicketReceiptPrintQuery(
            ticketId,
            request.format(),
            request.buyerLocale()
        ));
        var receiptConfig = resolveReceiptConfig(ctx);

        var rendered = documentApi.render(toRenderRequest(printContent, request));

        if (request.recordPrint()) {
            commandBus.execute(new RecordTicketPrintCommand(
                ticketId,
                request.format(),
                request.reprintReason()
            ));
        }

        if (request.shouldSendToBuyer()) {
            var printView = queryBus.ask(new GetTicketPrintViewQuery(ticketId));
            var outboundDocument = toCommunicationDocument(printView, request, rendered, receiptConfig);
            communicationMapper.toOutboundMessages(ctx, printView, outboundDocument, request)
                .forEach(communicationApi::enqueue);
        }

        return toFileResponse(rendered);
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

    private DocumentRenderRequest toRenderRequest(
        TicketReceiptPrintContent content,
        PrintTicketRequest request
    ) {
        var bodyLines = content.bodyLines().stream().map(DocumentLine::of).toList();
        return new DocumentRenderRequest(
            DocumentTemplateKey.of("sales.ticket.receipt." + request.format().name().toLowerCase() + ".v1"),
            DocumentKind.RECEIPT,
            toDocumentFormat(request.format()),
            content.title(),
            ReceiptDocumentContent.ofBodyLines(bodyLines),
            qrAssets(content.qrPayload(), request.format()),
            DocumentOptions.defaults(),
            content.locale(),
            null,
            content.metadata()
        );
    }

    private List<DocumentAsset> qrAssets(String qrPayload, PrintOutputFormat format) {
        if (qrPayload == null || qrPayload.isBlank()) {
            return List.of();
        }
        return List.of(DocumentAsset.qr(qrPayload, format == PrintOutputFormat.ESC_POS ? 280 : 300));
    }

    private DocumentFormat toDocumentFormat(PrintOutputFormat format) {
        return switch (format) {
            case PDF -> DocumentFormat.PDF;
            case ESC_POS -> DocumentFormat.ESC_POS;
        };
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
        TicketPrintView printView,
        PrintTicketRequest request,
        RenderedDocument rendered,
        TenantInternalDocumentConfig.ReceiptConfig receiptConfig
    ) {
        if (rendered.format() == DocumentFormat.PDF) {
            return rendered;
        }

        var pdfRequest = new PrintTicketRequest(
            PrintOutputFormat.PDF,
            false,
            request.reprintReason(),
            request.deliveryOptions(),
            request.buyerPhoneNumber(),
            request.buyerEmail(),
            request.buyerLocale()
        );

        return documentApi.render(documentMapper.toRenderRequest(printView, pdfRequest, receiptConfig));
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

    private void validateSellerContext(TchRequestContext ctx, java.util.UUID terminalId) {
        if (terminalId == null) {
            return;
        }
        sellerContextResolver.resolve(new ResolveSellerOperationalContextRequest(
            ctx,
            TerminalId.of(terminalId),
            SellerOperation.SELL
        ));
    }

    private void validateSellerContextForPrint(TchRequestContext ctx, PrintTicketRequest request) {
        // PrintTicketRequest does not currently carry a terminalId. Trusted context is enforced
        // upstream via Spring Security + TchContextFilter; revisit when terminalId moves into
        // the print payload.
    }
}
