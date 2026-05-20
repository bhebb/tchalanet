package com.tchalanet.server.core.sales.internal.application.service.print;

import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.sales.api.command.print.RecordTicketPrintCommand;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.Ticket;
import org.springframework.stereotype.Component;

@Component
public class TicketPrintPolicyService {

    private static final int MAX_REASON_LENGTH = 500;
    private static final int MIN_REPRINT_REASON_LENGTH = 10;

    public void requirePrintAllowed(Ticket ticket, RecordTicketPrintCommand command) {
        if (ticket == null) {
            throw new IllegalArgumentException("ticket is required");
        }
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }

        var saleStatus = ticket.lifecycle().sale().status();
        if (saleStatus == TicketSaleStatus.VOIDED) {
            throw ProblemRest.forbidden("ticket.print.voided_not_allowed");
        }
        if (!saleStatus.isAcceptedSale()) {
            throw ProblemRest.forbidden("ticket.print.sale_not_accepted");
        }

        var isReprint = ticket.print().printCount() > 0;
        var reason = command.reason();
        if (isReprint && (reason == null || reason.trim().length() < MIN_REPRINT_REASON_LENGTH)) {
            throw ProblemRest.badRequest("ticket.reprint.reason_required");
        }
        if (reason != null && reason.length() > MAX_REASON_LENGTH) {
            throw ProblemRest.badRequest("ticket.reprint.reason_too_long");
        }
    }
}

