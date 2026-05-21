package com.tchalanet.server.features.cashier.tickets.mapper;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintView;
import com.tchalanet.server.features.cashier.tickets.model.PrintDeliveryOption;
import com.tchalanet.server.features.cashier.tickets.model.PrintTicketRequest;
import com.tchalanet.server.platform.communication.api.model.request.SendOutboundMessageRequest;
import com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel;
import com.tchalanet.server.platform.communication.api.model.value.OutboundRecipient;
import com.tchalanet.server.platform.document.api.model.RenderedDocument;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class TicketPrintCommunicationMapper {

    private static final String MESSAGE_TYPE = "TICKET_RECEIPT";
    private static final String TEMPLATE_KEY = "ticket.receipt";
    private static final int MAX_INLINE_ATTACHMENT_BYTES = 200_000;

    public List<SendOutboundMessageRequest> toOutboundMessages(
        TchRequestContext ctx,
        TicketPrintView view,
        RenderedDocument rendered,
        PrintTicketRequest request
    ) {
        var locale = request.buyerLocale() == null ? defaultLocale(view) : request.buyerLocale();

        return request.deliveryOptions().stream()
            .distinct()
            .filter(PrintDeliveryOption::external)
            .map(option -> toOutboundMessage(ctx, view, rendered, locale, option, request))
            .toList();
    }

    private SendOutboundMessageRequest toOutboundMessage(
        TchRequestContext ctx,
        TicketPrintView view,
        RenderedDocument rendered,
        Locale locale,
        PrintDeliveryOption option,
        PrintTicketRequest request
    ) {
        var channel = toChannel(option);
        var recipient = new OutboundRecipient(
            ctx.effectiveTenantIdOrNull(),
            ctx.userId(),
            destination(option, request),
            null
        );

        return new SendOutboundMessageRequest(
            MESSAGE_TYPE,
            channel,
            recipient,
            locale,
            metadata(ctx, view, rendered, channel)
        );
    }

    private Map<String, Object> metadata(
        TchRequestContext ctx,
        TicketPrintView view,
        RenderedDocument rendered,
        CommunicationChannel channel
    ) {
        var metadata = new LinkedHashMap<String, Object>();
        metadata.put("templateKey", TEMPLATE_KEY);
        metadata.put("eventId", "ticket-print-" + view.identity().ticketId().value() + ":" + channel.name());
        metadata.put("correlationKey", "ticket-print:" + view.identity().ticketId().value() + ":" + channel.name());
        var baseIdem = ctx.idempotencyKey();
        metadata.put(
            "idempotencyKey",
            baseIdem == null || baseIdem.isBlank()
                ? "ticket-print:" + view.identity().ticketId().value() + ":" + channel.name()
                : baseIdem + ":" + channel.name());
        metadata.put("requestId", ctx.requestId());
        metadata.put("priority", "NORMAL");
        metadata.put("title", subject(view));
        metadata.put("subject", subject(view));
        metadata.put("message", message(view));
        metadata.put("body", message(view));
        metadata.put("ticketId", view.identity().ticketId().value().toString());
        metadata.put("ticketCode", view.identity().ticketCode());
        metadata.put("publicCode", view.identity().publicCode());
        metadata.put("drawName", view.draw().label());
        metadata.put("totalAmount", view.money().totalAmount().toString());
        metadata.put("verificationCode", view.identity().verificationCode());
        metadata.put("verificationUrl", view.qr().verificationUrl());
        metadata.put("channel", channel.name());
        metadata.put("documentFilename", rendered.filename());
        metadata.put("documentContentType", rendered.contentType());
        metadata.put("documentFormat", rendered.format().name());

        if (supportsAttachment(channel)) {
            if (rendered.bytes().length > MAX_INLINE_ATTACHMENT_BYTES) {
                throw new IllegalStateException("Document too large for inline communication attachment");
            }
            metadata.put("attachments", List.of(Map.of(
                "filename", rendered.filename(),
                "contentType", rendered.contentType(),
                "contentBase64", Base64.getEncoder().encodeToString(rendered.bytes()),
                "disposition", "attachment"
            )));
        }

        return Map.copyOf(metadata);
    }

    private boolean supportsAttachment(CommunicationChannel channel) {
        return channel == CommunicationChannel.EMAIL || channel == CommunicationChannel.WHATSAPP;
    }

    private CommunicationChannel toChannel(PrintDeliveryOption option) {
        return switch (option) {
            case SMS -> CommunicationChannel.SMS;
            case WHATSAPP -> CommunicationChannel.WHATSAPP;
            case EMAIL -> CommunicationChannel.EMAIL;
            case RETURN_FILE -> throw new IllegalArgumentException("RETURN_FILE is not an external channel");
        };
    }

    private String destination(PrintDeliveryOption option, PrintTicketRequest request) {
        return switch (option) {
            case SMS, WHATSAPP -> request.buyerPhoneNumber();
            case EMAIL -> request.buyerEmail();
            case RETURN_FILE -> null;
        };
    }

    private Locale defaultLocale(TicketPrintView view) {
        return view.metadata() == null || view.metadata().locale() == null
            ? Locale.FRENCH
            : view.metadata().locale();
    }

    private String subject(TicketPrintView view) {
        return "Ticket Tchalanet " + view.identity().ticketCode();
    }

    private String message(TicketPrintView view) {
        return """
            Votre ticket Tchalanet %s est disponible.
            Tirage: %s
            Montant total: %s
            Vérification: %s
            Code: %s
            """.formatted(
            view.identity().ticketCode(),
            view.draw().label(),
            view.money().totalAmount(),
            view.qr().verificationUrl(),
            view.identity().verificationCode()
        );
    }
}
