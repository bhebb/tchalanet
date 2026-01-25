package com.tchalanet.server.core.sales.application.query.handler;

import com.tchalanet.server.catalog.address.domain.model.Address;
import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.catalog.address.application.port.out.AddressReaderPort;
import com.tchalanet.server.core.outlet.application.port.out.OutletReaderPort;
import com.tchalanet.server.core.pos.application.port.out.TerminalReaderPort;
import com.tchalanet.server.core.sales.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.application.query.model.VerifyPublicTicketQuery;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import com.tchalanet.server.core.sales.domain.model.TicketVerificationResult;
import com.tchalanet.server.catalog.settings_bk.AppSettingsResolver;
import com.tchalanet.server.catalog.settings_bk.registry.AppSettingRegistry;
import com.tchalanet.server.catalog.settings_bk.query.ResolveAppSettingsQuery;
import com.tchalanet.server.common.types.id.TenantId;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * Handler to verify a ticket by public code and return verification details.
 *
 * <p>Security notes: - Rate-limit at controller layer to prevent abuse. - Add noindex meta tag or
 * header to prevent search engine indexing.
 */
@UseCase
@RequiredArgsConstructor
public class VerifyPublicTicketQueryHandler
    implements QueryHandler<VerifyPublicTicketQuery, TicketVerificationResult> {

  private final TicketReaderPort ticketReader;
  private final TerminalReaderPort terminalReader;
  private final OutletReaderPort outletReader;
  private final AddressReaderPort addressReader;
  private final AppSettingsResolver appSettingsResolver;

  @Override
  public TicketVerificationResult handle(VerifyPublicTicketQuery query) {
    Instant now = query.now() != null ? query.now() : Instant.now();
    var optTicket =
        ticketReader
            .findByPublicCode(query.publicCode());

    if (optTicket.isEmpty()) {
      // unknown code → 404 handled by controller (return null here)
      return null;
    }

    var baseTicket = optTicket.get();
    var visibilityDays = resolveVisibilityDays(baseTicket.getTenantId());
    var visible = isVisible(baseTicket, now, visibilityDays);

    var ticketWithLines = ticketReader.findWithLinesById(baseTicket.getId()).orElse(baseTicket);
    var result = toResult(ticketWithLines);

    if (!visible) {
      // existing but outside window → EXPIRED (not 404)
      return new TicketVerificationResult(
          result.ticketId(),
          result.publicCode(),
          com.tchalanet.server.core.sales.domain.model.TicketStatus.EXPIRED,
          result.drawId(),
          result.terminalMasked(),
          result.createdAt(),
          result.totalAmount(),
          result.potentialTotalPayout(),
          result.payoutStatus(),
          result.outletName(),
          result.outletAddress(),
          result.lines());
    }

    return result;
  }

  private int resolveVisibilityDays(TenantId tenantId) {
    try {
      var settings =
          appSettingsResolver.resolve(
              new ResolveAppSettingsQuery(tenantId, List.of(AppSettingRegistry.TICKET_PUBLIC_VISIBILITY_DAYS.namespace())));
      return settings.stream()
          .filter(s -> AppSettingRegistry.TICKET_PUBLIC_VISIBILITY_DAYS.matches(s.namespace(), s.key()))
          .findFirst()
          .map(s -> (Integer) s.value())
          .orElse(AppSettingRegistry.TICKET_PUBLIC_VISIBILITY_DAYS.defaultValue());
    } catch (Exception e) {
      return AppSettingRegistry.TICKET_PUBLIC_VISIBILITY_DAYS.defaultValue();
    }
  }

  private boolean isVisible(Ticket ticket, Instant now, int visibilityDays) {
    var createdAt = ticket.getCreatedAt();
    if (createdAt == null) return false;
    var window = Duration.ofDays(Math.max(1, visibilityDays));
    return createdAt.plus(window).isAfter(now);
  }

  private TicketVerificationResult toResult(Ticket ticket) {
    var lines =
        ticket.getLines().stream()
            .map(
                l ->
                    new TicketVerificationResult.Line(
                        l.gameCode(), l.selection(), l.stake(), l.potentialPayout()))
            .toList();

    BigDecimal potentialTotal =
        lines.stream()
            .map(l -> l.potentialPayout() == null ? BigDecimal.ZERO : l.potentialPayout())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    String payoutStatus = potentialTotal.signum() > 0 ? "POTENTIAL_WIN" : "NO_PAYOUT";

    // try to resolve outlet and its address from terminal -> outlet
    String outletName = null;
    var outletAddress = (Address) null;
    try {
      var terminalOpt =
          terminalReader.findById(
              ticket.getTenantId(), TerminalId.of(ticket.getTerminalId().uuid()));
      if (terminalOpt.isPresent()) {
        var terminal = terminalOpt.get();
        var outletId = terminal.outletId();
        if (outletId != null) {
          var outletOpt = outletReader.findById(outletId, ticket.getTenantId());
          if (outletOpt.isPresent()) {
            var outlet = outletOpt.get();
            outletName = outlet.name();
            if (outlet.addressId() != null) {
              outletAddress = addressReader.findById(outlet.addressId()).orElse(null);
            }
          }
        }
      }
    } catch (Exception ignored) {
      // best effort: don't fail verification for missing outlet info
    }

    return new TicketVerificationResult(
        ticket.getId(),
        ticket.getPublicCode(),
        ticket.getStatus(),
        ticket.getDrawId(),
        maskTerminal(ticket.getTerminalId()),
        ticket.getCreatedAt(),
        ticket.getTotalAmount(),
        potentialTotal,
        payoutStatus,
        outletName,
        outletAddress,
        lines);
  }

  private String maskTerminal(TerminalId terminalId) {
    if (terminalId == null) return null;
    var s = terminalId.toString();
    return s.length() <= 8 ? s : s.substring(0, 8) + "…";
  }
}
