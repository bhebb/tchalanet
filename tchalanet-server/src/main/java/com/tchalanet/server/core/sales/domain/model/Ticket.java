package com.tchalanet.server.core.sales.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate Root for the Ticket domain. It encapsulates the state and business rules for a ticket,
 * ensuring its consistency.
 */
public class Ticket {

    private final UUID id;
    private final UUID tenantId;
    private final UUID terminalId;
    private final UUID sessionId; // nullable pour compat
    private final UUID drawId;
    private final String ticketCode;
    private final String publicCode;
    private TicketStatus status;
    private final BigDecimal totalAmount;
    private final Instant createdAt;
    private Instant updatedAt;
    private final List<TicketLine> lines;
    private BigDecimal totalStake;
    // potentialPayout remains per-line (TicketLine.potentialPayout). Aggregate winning amount after result:
    private BigDecimal winningAmount;
    // projection fields removed from domain — they belong to DTO/projection layers

    private Ticket(
        UUID id,
        UUID tenantId,
        UUID terminalId,
        UUID sessionId,
        UUID drawId,
        String ticketCode,
        String publicCode,
        List<TicketLine> lines,
        BigDecimal totalAmount,
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
        this.status = TicketStatus.SOLD;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Factory method to reconstruct or create Ticket. Caller supplies createdAt (Clock).
     */
    public static Ticket create(
        UUID tenantId,
        UUID terminalId,
        UUID sessionId,
        UUID drawId,
        String ticketCode,
        String publicCode,
        List<TicketLine> lines,
        Instant createdAt) {
        Objects.requireNonNull(tenantId, "TenantId cannot be null");
        Objects.requireNonNull(terminalId, "TerminalId cannot be null");
        Objects.requireNonNull(drawId, "DrawId cannot be null");
        Objects.requireNonNull(ticketCode, "TicketCode cannot be null");
        Objects.requireNonNull(publicCode, "PublicCode cannot be null");
        Objects.requireNonNull(createdAt, "createdAt cannot be null");
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("A ticket must have at least one line.");
        }
        BigDecimal totalAmount = lines.stream().map(TicketLine::stake).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Ticket(UUID.randomUUID(), tenantId, terminalId, sessionId, drawId, ticketCode, publicCode, lines, totalAmount, createdAt, createdAt);
    }

    // --- Business Methods ---

    public void markAsPaid(Instant when) {
        Objects.requireNonNull(when, "when");
        if (this.status != TicketStatus.RESULTED_WIN) {
            throw new IllegalStateException(
                "Only a RESULTED_WIN ticket can be marked as PAID. Current status: " + this.status);
        }
        this.status = TicketStatus.PAID;
        this.updatedAt = when;
    }

    public void voidTicket(Instant when) {
        Objects.requireNonNull(when, "when");
        if (this.status != TicketStatus.SOLD) {
            throw new IllegalStateException(
                "Only a SOLD ticket can be voided. Current status: " + this.status);
        }
        this.status = TicketStatus.VOIDED;
        this.updatedAt = when;
    }

    /**
     * Apply draw result to this ticket. Computes/sets the winning amount and transitions the ticket
     * to RESULTED_WIN or RESULTED_LOSS. Allowed only when ticket is SOLD.
     */
    public void recordResult(BigDecimal winningAmount, Instant when) {
        Objects.requireNonNull(when, "when");
        Objects.requireNonNull(winningAmount, "winningAmount");
        if (this.status != TicketStatus.SOLD) {
            throw new IllegalStateException("recordResult allowed only when ticket is SOLD. Current status: " + this.status);
        }
        this.winningAmount = winningAmount;
        if (winningAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.status = TicketStatus.RESULTED_WIN;
        } else {
            this.status = TicketStatus.RESULTED_LOSS;
        }
        this.updatedAt = when;
    }

    // --- Getters (explicit) ---
    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getTerminalId() { return terminalId; }
    public UUID getSessionId() { return sessionId; }
    public UUID getDrawId() { return drawId; }
    public String getTicketCode() { return ticketCode; }
    public String getPublicCode() { return publicCode; }
    public TicketStatus getStatus() { return status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<TicketLine> getLines() { return List.copyOf(lines); }
    public BigDecimal getTotalStake() { return totalStake; }
    public BigDecimal getWinningAmount() { return winningAmount; }

    public void setTotalStake(BigDecimal totalStake) { this.totalStake = totalStake; }
    // No setters for projection fields — projections should be built outside the aggregate

}
