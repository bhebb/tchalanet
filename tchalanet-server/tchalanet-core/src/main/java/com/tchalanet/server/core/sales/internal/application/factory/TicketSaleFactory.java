package com.tchalanet.server.core.sales.internal.application.factory;

import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.ApprovalRequestId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.draw.internal.application.query.projection.DrawSummary;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketNumberGeneratorPort;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketPublicCodeGeneratorPort;
import com.tchalanet.server.core.sales.internal.domain.model.Ticket;
import com.tchalanet.server.core.sales.internal.domain.model.TicketLine;
import com.tchalanet.server.core.session.internal.domain.model.SalesSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Currency;
import java.util.List;

import static com.tchalanet.server.common.constant.CommonConstants.DEFAULT_CURRENCY;

@Component
@RequiredArgsConstructor
public class TicketSaleFactory {

    private final TicketNumberGeneratorPort numberGenerator;
    private final TicketPublicCodeGeneratorPort publicCodeGenerator;
    private final IdGenerator idGenerator;

    public Ticket newSoldTicket(
        TenantId tenantId,
        TerminalId terminalId,
        SalesSession session,
        DrawSummary draw,
        List<TicketLine> lines,
        Currency currency,
        Instant now) {

        String ticketCode = numberGenerator.generate();
        String publicCode = publicCodeGenerator.generate();

        return Ticket.sell(
            TicketId.of(idGenerator.newUuid()),
            tenantId,
            terminalId,
            session.id(),
            draw.drawId(),
            ticketCode,
            publicCode,
            currency,
            lines,
            now);
    }

    public Ticket newPendingApprovalTicket(TenantId tenantId,
                                           TerminalId terminalId,
                                           SalesSession session,
                                           DrawSummary draw,
                                           List<TicketLine> lines,
                                           ApprovalRequestId approvalRequestId,
                                           Instant now) {

        String ticketCode = numberGenerator.generate();
        String publicCode = publicCodeGenerator.generate();

        return Ticket.requestApproval(
            TicketId.of(idGenerator.newUuid()),
            tenantId,
            terminalId,
            session.id(),
            draw.drawId(),
            ticketCode,
            publicCode,
            DEFAULT_CURRENCY,
            lines,
            now,
            approvalRequestId);
    }
}
