package com.tchalanet.server.core.sales.internal.application.service.communication;


import com.tchalanet.server.common.types.id.CorrelationId;
import com.tchalanet.server.core.sales.api.model.communication.SaleCommunicationOptions;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.Ticket;
import com.tchalanet.server.platform.communication.api.CommunicationApi;
import com.tchalanet.server.platform.communication.api.model.request.SendOutboundMessageRequest;
import com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel;
import com.tchalanet.server.platform.communication.api.model.value.OutboundRecipient;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TicketCommunicationRequestDispatcher {

    private static final String MESSAGE_TYPE_TICKET_PLACED = "ticket.placed";

    private final CommunicationApi communicationApi;

    public void enqueueTicketPlaced(
        Ticket ticket,
        SaleCommunicationOptions options,
        CorrelationId correlationId
    ) {
        if (options == null || options.isEmpty()) {
            return;
        }

        var locale = options.buyerLocale() == null ? Locale.FRENCH : options.buyerLocale();

        if (options.sendSms()) {
            enqueue(ticket, CommunicationChannel.SMS, options.buyerPhoneNumber(), locale, correlationId);
        }

        if (options.sendWhatsapp()) {
            enqueue(ticket, CommunicationChannel.WHATSAPP, options.buyerPhoneNumber(), locale, correlationId);
        }

        if (options.sendEmail()) {
            enqueue(ticket, CommunicationChannel.EMAIL, options.buyerEmail(), locale, correlationId);
        }
    }

    private void enqueue(
        Ticket ticket,
        CommunicationChannel channel,
        String destination,
        Locale locale,
        CorrelationId correlationId
    ) {
        var metadata = ticketMetadata(ticket, correlationId, channel);

        var request = new SendOutboundMessageRequest(
            MESSAGE_TYPE_TICKET_PLACED,
            channel,
            OutboundRecipient.of(destination),
            locale,
            metadata
        );

        communicationApi.enqueue(request);
    }

    private Map<String, Object> ticketMetadata(
        Ticket ticket,
        CorrelationId correlationId,
        CommunicationChannel channel
    ) {
        var metadata = new HashMap<String, Object>();

        metadata.put("ticketId", ticket.identity().id().value().toString());
        metadata.put("ticketCode", ticket.codes().ticketCode().value());
        metadata.put("publicCode", ticket.codes().publicCode().value());
        metadata.put("verificationCode", ticket.codes().verificationCode().value());
        metadata.put("currency", ticket.money().currency().value());
        metadata.put("totalAmount", ticket.money().breakdown().total().amount());
        metadata.put("placedAt", ticket.lifecycle().sale().placedAt().toString());
        metadata.put("drawId", ticket.context().drawId().value().toString());

        if (correlationId != null) {
            metadata.put("correlationId", correlationId.value());
        }

        metadata.put("idempotencyKey",
            "sales.ticket-placed:"
                + ticket.identity().id().value()
                + ":"
                + channel.name()
        );

        return Map.copyOf(metadata);
    }
}
