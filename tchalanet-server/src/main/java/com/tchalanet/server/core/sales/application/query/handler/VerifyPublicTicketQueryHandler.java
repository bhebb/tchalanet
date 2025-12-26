package com.tchalanet.server.core.sales.application.query.handler;
import com.tchalanet.server.common.types.id.TerminalId;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.application.query.model.VerifyPublicTicketQuery;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import com.tchalanet.server.core.sales.domain.model.TicketVerificationResult;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;

/**
 * Handler to verify a ticket by public code and return verification details.
 *
 * Security notes:
 * - Rate-limit at controller layer to prevent abuse.
 * - Add noindex meta tag or header to prevent search engine indexing.
 */
@UseCase
@RequiredArgsConstructor
public class VerifyPublicTicketQueryHandler
    implements QueryHandler<VerifyPublicTicketQuery, TicketVerificationResult> {

  private final TicketReaderPort ticketReader;
  // TODO: replace with tenant config visibility window days (7–30)
  private static final Duration DEFAULT_VISIBILITY = Duration.ofDays(30);

  @Override
  public TicketVerificationResult handle(VerifyPublicTicketQuery query) {
    Instant now = query.now() != null ? query.now() : Instant.now();

    var ticket =
        ticketReader
            .findByPublicCode(query.publicCode())
            .filter(t -> isVisible(t, now))
            .flatMap(t -> ticketReader.findWithLinesById(t.getId()));

    return ticket.map(this::toResult).orElse(null);
  }

  private boolean isVisible(Ticket ticket, Instant now) {
    var createdAt = ticket.getCreatedAt();
    if (createdAt == null) return false;
    return createdAt.plus(DEFAULT_VISIBILITY).isAfter(now);
  }

  private TicketVerificationResult toResult(Ticket ticket) {
    var lines =
        ticket.getLines().stream()
            .map(
                l ->
                    new TicketVerificationResult.Line(
                        l.gameCode(), l.selection(), l.stake(), l.potentialPayout()))
            .toList();

    return new TicketVerificationResult(
        ticket.getId(),
        ticket.getPublicCode(),
        ticket.getStatus(),
        ticket.getDrawId(),
        maskTerminal(ticket.getTerminalId()),
        ticket.getCreatedAt(),
        ticket.getTotalAmount(),
        lines);
  }

  private String maskTerminal(TerminalId terminalId) {
    if (terminalId == null) return null;
    var s = terminalId.toString();
    return s.length() <= 8 ? s : s.substring(0, 8) + "…";
  }
}
