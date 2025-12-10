package com.tchalanet.server.core.sales.application.query.handler;

import com.tchalanet.server.common.app.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.application.query.model.VerifyPublicTicketQuery;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import com.tchalanet.server.core.sales.domain.model.TicketLine;
import com.tchalanet.server.core.sales.domain.model.TicketVerificationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Handler to verify a ticket by public code and return verification details.
 */
@UseCase
@RequiredArgsConstructor
@Component
public class VerifyPublicTicketQueryHandler implements QueryHandler<VerifyPublicTicketQuery, Optional<TicketVerificationResult>> {

    private final TicketReaderPort ticketReaderPort;
    // plus tard : injecter un service de config/tenant pour la fenêtre
    private static final Duration DEFAULT_VISIBILITY = Duration.ofDays(30);

    @Override
    public Optional<TicketVerificationResult> handle(VerifyPublicTicketQuery query) {
        return ticketReaderPort.findByPublicCode(query.publicCode())
            .filter(ticket -> isVisible(ticket, query.now()))
            .map(this::toVerificationResult);
    }

    private boolean isVisible(Ticket ticket, Instant now) {
        // TODO: remplacer par config tenant : visibilityWindowDays
        var createdAt = ticket.getCreatedAt();
        if (createdAt == null) {
            return false;
        }
        return createdAt.plus(DEFAULT_VISIBILITY).isAfter(now);
    }

    private TicketVerificationResult toVerificationResult(Ticket ticket) {
        // TODO: adapter selon ta vraie structure de Ticket / TicketLine
        List<String> linesNumbers = ticket.getLines().stream()
            .map(this::formatNumbers)
            .toList();

        BigDecimal stakeAmount = ticket.getTotalStake();
        BigDecimal potentialPayout = ticket.getPotentialPayout();

        String outletNameMasked = maskOutletName(ticket.getOutletName());

        return new TicketVerificationResult(
            ticket.getId(),
            ticket.getTenantId(),
            ticket.getPublicCode(),
            ticket.getStatus(),
            ticket.getGameCode(),
            ticket.getDrawCode(),
            ticket.getDrawDateTime(),
            linesNumbers,
            stakeAmount,
            potentialPayout,
            outletNameMasked,
            ticket.getCreatedAt()
        );
    }

    private String formatNumbers(TicketLine line) {
        // exemple : "05-12-24-31"
        return String.join("-", line.getNumbers());
    }

    private String maskOutletName(String outletName) {
        if (outletName == null || outletName.length() < 2) {
            return outletName;
        }
        return outletName.charAt(0) + "***";
    }
}

