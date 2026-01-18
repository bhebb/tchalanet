package com.tchalanet.server.core.sales.domain.service;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.core.draw.domain.model.Draw;
import com.tchalanet.server.core.sales.application.port.out.TicketNumberGeneratorPort;
import com.tchalanet.server.core.sales.application.port.out.TicketPublicCodeGeneratorPort;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import com.tchalanet.server.core.sales.domain.model.TicketLine;
import com.tchalanet.server.core.session.domain.model.PosSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

import static com.tchalanet.server.common.constant.CommonConstants.DEFAULT_CURRENCY;

@Component
@RequiredArgsConstructor
public class TicketSaleFactory {

    private final TicketNumberGeneratorPort numberGenerator;
    private final TicketPublicCodeGeneratorPort publicCodeGenerator;

    public Ticket newSoldTicket(
        TenantId tenantId,
        TerminalId terminalId,
        PosSession session,
        Draw draw,
        List<TicketLine> lines,
        Instant now) {

        String ticketCode = numberGenerator.generate();
        String publicCode = publicCodeGenerator.generate();

        return Ticket.sell(
            tenantId,
            terminalId,
            session.id(),
            draw.id(),
            ticketCode,
            publicCode,
            DEFAULT_CURRENCY,
            lines,
            now);
    }

    public Ticket newPendingApprovalTicket(TenantId tenantId,
                                           TerminalId terminalId,
                                           PosSession session,
                                           Draw draw,
                                           List<TicketLine> lines,
                                           Instant now) {

        String ticketCode = numberGenerator.generate();
        String publicCode = publicCodeGenerator.generate();

        return Ticket.pendingApproval(
            tenantId,
            terminalId,
            session.id(),
            draw.id(),
            ticketCode,
            publicCode,
            DEFAULT_CURRENCY,
            lines,
            now);
    }
}

