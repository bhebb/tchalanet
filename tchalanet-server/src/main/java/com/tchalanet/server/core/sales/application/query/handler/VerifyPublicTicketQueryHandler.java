package com.tchalanet.server.core.sales.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.core.address.application.port.AddressReaderPort;
import com.tchalanet.server.core.outlet.application.port.out.OutletReaderPort;
import com.tchalanet.server.core.terminal.application.port.out.TerminalReaderPort;
import com.tchalanet.server.core.sales.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.application.query.model.VerifyPublicTicketQuery;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import com.tchalanet.server.core.sales.domain.model.TicketVerificationResult;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.catalog.settings.api.SettingsCatalog;
import com.tchalanet.server.catalog.settings.api.model.ResolveSettingsCriteria;
import com.tchalanet.server.catalog.settings.api.model.ResolvedSettingView;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import com.tchalanet.server.core.address.domain.Address;
import com.tchalanet.server.common.types.id.AddressId;
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
  private final SettingsCatalog settingsCatalog;

  @Override
  public TicketVerificationResult handle(VerifyPublicTicketQuery query) {
    Instant now = query.now() != null ? query.now() : Instant.now();

    String code = query.publicCode() == null ? "" : query.publicCode().trim();
    if (code.isEmpty()) return null;

    // Normalize Crockford input a bit (optional but helpful)
    code = code.toUpperCase(java.util.Locale.ROOT).replace("-", "").replace(" ", "");

    var optTicket = ticketReader.findByPublicCode(code);
    if (optTicket.isEmpty()) return null;

    var ticket = optTicket.get();

    // No explicit deleted flag on Ticket domain here; archived rows should be handled by reader/DB

    int visibilityDays = resolveVisibilityDays(ticket.getTenantId());
    boolean visible = isVisible(ticket, now, visibilityDays);

    if (!visible) {
      // minimal payload: use record constructor and keep sensitive fields null/empty
      return new TicketVerificationResult(
          ticket.getId(), // ticketId
          ticket.getPublicCode(), // publicCode
          null, // saleStatus
          null, // resultStatus
          null, // settlementStatus
          null, // drawId
          maskTerminal(ticket.getTerminalId()), // terminalMasked
          ticket.getCreatedAt(), // createdAt
          null, // totalAmount
          null, // potentialTotalPayout
          "EXPIRED", // payoutStatus
          null, // outletName
          null, // outletAddress
          List.of() // lines
      );
    }

    return toVisibleResult(ticket);
  }

  private TicketVerificationResult toVisibleResult(Ticket ticket) {
    var lines =
        ticket.getLines().stream()
            .map(l -> new TicketVerificationResult.Line(
                l.gameCode().name(),
                l.selection(),
                l.stake(),
                l.potentialPayout()))
            .toList();

    BigDecimal potentialTotal =
        lines.stream()
            .map(l -> l.potentialPayout() == null ? BigDecimal.ZERO : l.potentialPayout())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    String payoutStatus = potentialTotal.signum() > 0 ? "POTENTIAL_WIN" : "NO_PAYOUT";

    // best effort outlet/address (but I'd mask)
    String outletName = null;
    Address outletAddress = null;

    try {
      var terminalOpt = terminalReader.findById(ticket.getTenantId(), ticket.getTerminalId());
      if (terminalOpt.isPresent()) {
        var terminal = terminalOpt.get();
        var outletId = terminal.outletId();
        if (outletId != null) {
          var outletOpt = outletReader.findById(outletId);
          if (outletOpt.isPresent()) {
            var outlet = outletOpt.get();
            outletName = outlet.name();

            if (outlet.addressId() != null) {
              outletAddress =
                  addressReader
                      .findById(ticket.getTenantId(), AddressId.of(outlet.addressId()))
                      .orElse(null);
            }
          }
        }
      }
    } catch (Exception ignored) {}

    return new TicketVerificationResult(
        ticket.getId(),
        ticket.getPublicCode(),
        ticket.getSaleStatus(),
        ticket.getResultStatus(),
        ticket.getSettlementStatus(),
        ticket.getDrawId(),
        maskTerminal(ticket.getTerminalId()),
        ticket.getCreatedAt(),
        ticket.getTotalAmount(),
        potentialTotal,
        payoutStatus,
        outletName,
        maskAddress(outletAddress), // ✅ mask recommended
        lines
    );
  }

  private Address maskAddress(Address a) {
    if (a == null) return null;
    // MVP: only keep city + country to reduce leakage
    return new Address(
        a.id(),
        a.tenantId(),
        null, // line1
        null, // line2
        a.city(),
        null, // region/state
        a.country(),
        null, // postalCode
        null, // normalizedKey
        false, // deleted
        null, // createdAt
        null  // updatedAt
    );
  }

  private int resolveVisibilityDays(TenantId tenantId) {
    try {
      List<ResolvedSettingView> resolved =
          settingsCatalog.resolve(ResolveSettingsCriteria.forTenant(tenantId, List.of("ticket.verification")));
      return resolved.stream()
          .filter(s -> "public_visibility_days".equals(s.settingKey()))
          .findFirst()
          .map(s -> {
            try { return Integer.parseInt(s.settingValue()); } catch (Exception e) { return null; }
          })
          .orElse(14);
    } catch (Exception e) {
      return 14;
    }
  }

  private boolean isVisible(Ticket ticket, Instant now, int visibilityDays) {
    var createdAt = ticket.getCreatedAt();
    if (createdAt == null) return false;
    var window = Duration.ofDays(Math.max(1, visibilityDays));
    return createdAt.plus(window).isAfter(now);
  }

  private String maskTerminal(TerminalId terminalId) {
    if (terminalId == null) return null;
    var s = terminalId.toString();
    return s.length() <= 8 ? s : s.substring(0, 8) + "…";
  }
}
