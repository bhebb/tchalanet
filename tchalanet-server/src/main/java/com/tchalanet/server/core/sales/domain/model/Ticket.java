package com.tchalanet.server.core.sales.domain.model;

import com.tchalanet.server.common.types.enums.TicketStatus;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.SessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import lombok.Getter;

/**
 * Aggregate Root for the Ticket domain. It encapsulates the state and business rules for a ticket,
 * ensuring its consistency.
 */
@Getter
public class Ticket {

  private final TicketId id;
  private final TenantId tenantId;
  private final TerminalId terminalId;
  private final SessionId sessionId;
  private final DrawId drawId;
  private final String ticketCode;
  private final String publicCode;

  private TicketStatus status;
  private final BigDecimal totalAmount;

  private final Instant createdAt;
  private Instant updatedAt;

  private final List<TicketLine> lines;
  private BigDecimal winningAmount;

  private Ticket(
      TicketId id,
      TenantId tenantId,
      TerminalId terminalId,
      SessionId sessionId,
      DrawId drawId,
      String ticketCode,
      String publicCode,
      List<TicketLine> lines,
      BigDecimal totalAmount,
      TicketStatus status,
      Instant createdAt,
      Instant updatedAt) {
    this.id = id;
    this.tenantId = tenantId;
    this.terminalId = terminalId;
    this.sessionId = sessionId;
    this.drawId = drawId;
    this.ticketCode = ticketCode;
    this.publicCode = publicCode;
    this.lines = lines;
    this.totalAmount = totalAmount;
    this.status = status;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public static Ticket create(
      TenantId tenantId,
      TerminalId terminalId,
      SessionId sessionId,
      DrawId drawId,
      String ticketCode,
      String publicCode,
      List<TicketLine> lines,
      Instant createdAt) {
    Objects.requireNonNull(tenantId);
    Objects.requireNonNull(terminalId);
    Objects.requireNonNull(drawId);
    Objects.requireNonNull(ticketCode);
    Objects.requireNonNull(publicCode);
    Objects.requireNonNull(createdAt);
    if (lines == null || lines.isEmpty())
      throw new IllegalArgumentException("ticket must have >= 1 line");

    BigDecimal total =
        lines.stream().map(TicketLine::stake).reduce(BigDecimal.ZERO, BigDecimal::add);

    return new Ticket(
        TicketId.random(),
        tenantId,
        terminalId,
        sessionId,
        drawId,
        ticketCode,
        publicCode,
        List.copyOf(lines),
        total,
        TicketStatus.PENDING,
        createdAt,
        createdAt);
  }

  /** Rehydrate from persistence */
  public static Ticket rehydrate(
      TicketId id,
      TenantId tenantId,
      TerminalId terminalId,
      SessionId sessionId,
      DrawId drawId,
      String ticketCode,
      String publicCode,
      List<TicketLine> lines,
      BigDecimal totalAmount,
      TicketStatus status,
      Instant createdAt,
      Instant updatedAt) {
    Objects.requireNonNull(id);
    Objects.requireNonNull(tenantId);
    Objects.requireNonNull(terminalId);
    Objects.requireNonNull(drawId);
    Objects.requireNonNull(ticketCode);
    Objects.requireNonNull(status);
    Objects.requireNonNull(createdAt);
    Objects.requireNonNull(updatedAt);

    return new Ticket(
        id,
        tenantId,
        terminalId,
        sessionId,
        drawId,
        ticketCode,
        publicCode,
        lines == null ? List.of() : List.copyOf(lines),
        totalAmount == null ? BigDecimal.ZERO : totalAmount,
        status,
        createdAt,
        updatedAt);
  }

  // ---------- State machine methods ----------

  public void voidTicket(Instant when) {
    requireWhen(when);
    ensureStatus(TicketStatus.PENDING);
    this.status = TicketStatus.VOID;
    this.updatedAt = when;
  }

  public void recordResult(BigDecimal winningAmount, Instant when) {
    requireWhen(when);
    Objects.requireNonNull(winningAmount, "winningAmount");
    if (winningAmount.signum() < 0)
      throw new IllegalArgumentException("winningAmount cannot be negative");
    ensureStatus(TicketStatus.PENDING);

    this.winningAmount = winningAmount;
    this.status = winningAmount.signum() > 0 ? TicketStatus.WON : TicketStatus.LOST;
    this.updatedAt = when;
  }

  public void markPaymentPending(Instant when) {
    requireWhen(when);
    ensureStatus(TicketStatus.WON);
    this.status = TicketStatus.PENDING;
    this.updatedAt = when;
  }

  public void markAsPaid(Instant when) {
    requireWhen(when);
    if (this.status != TicketStatus.WON && this.status != TicketStatus.PENDING) {
      throw new IllegalStateException(
          "PAID allowed only from RESULTED_WIN or PAYMENT_PENDING. status=" + this.status);
    }
    this.status = TicketStatus.PAID;
    this.updatedAt = when;
  }

  private void ensureStatus(TicketStatus expected) {
    if (this.status != expected) {
      throw new IllegalStateException("Expected status " + expected + " but was " + this.status);
    }
  }

  private static void requireWhen(Instant when) {
    Objects.requireNonNull(when, "when");
  }

  public List<TicketLine> getLines() {
    return List.copyOf(lines);
  }
}
