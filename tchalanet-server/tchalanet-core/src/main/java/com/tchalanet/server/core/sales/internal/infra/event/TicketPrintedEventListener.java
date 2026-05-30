package com.tchalanet.server.core.sales.internal.infra.event;

import com.tchalanet.server.core.sales.api.event.TicketPrintedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

@Slf4j
@Component
public class TicketPrintedEventListener {

    // Temporary in-memory idempotence until sales_ticket_print_log persistence is introduced.
    private static final Set<UUID> PROCESSED_EVENT_IDS = ConcurrentHashMap.newKeySet();

    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void onTicketPrinted(TicketPrintedEvent event) {
        if (event == null || event.eventId() == null || event.eventId().value() == null) {
            log.warn("Skip ticket printed event without eventId: {}", event);
            return;
        }

        var eventId = event.eventId().value();
        if (!PROCESSED_EVENT_IDS.add(eventId)) {
            log.debug("TicketPrintedEvent already processed: eventId={}", eventId);
            return;
        }

        var hasReason = event.reason() != null && !event.reason().isBlank();

        // TODO(v1.5): replace this log-only listener with persistent sales_ticket_print_log writes.
        log.info(
            "Ticket printed: eventId={}, tenantId={}, ticketId={}, printedBy={}, outputFormat={}, paperSize={}, reprint={}, reason={}",
            eventId,
            event.tenantId(),
            event.ticketId(),
            event.printedBy(),
            event.outputFormat(),
            event.paperSize(),
            hasReason,
            hasReason ? event.reason() : ""
        );
    }
}

