package com.tchalanet.server.core.sales.application.port.out;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Use case for printing a ticket.
 */
public interface PrintTicketModels {

    /**
     * Payload containing all data needed to render a ticket for printing.
     */
    record PrintTicketPayload(
        String ticketCode,
        String publicCode,
        String terminalId,
        String drawId,
        Instant createdAt,
        List<Line> lines,
        BigDecimal totalAmount
    ) {

        /**
         * Represents a single line on the ticket.
         */
        public record Line(
            String gameCode,
            String selection,
            BigDecimal stake,
            BigDecimal oddsSnapshot
        ) {}
    }
}
