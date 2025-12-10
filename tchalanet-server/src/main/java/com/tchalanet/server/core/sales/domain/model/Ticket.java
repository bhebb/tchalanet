package com.tchalanet.server.core.sales.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
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
    private BigDecimal potentialPayout;
    private String outletName;
    private String gameCode;
    private String drawCode;
    private Instant drawDateTime;

    private Ticket(
        UUID id,
        UUID tenantId,
        UUID terminalId,
        UUID sessionId,
        UUID drawId,
        String ticketCode,
        String publicCode,
        List<TicketLine> lines) {
        this.id = id;
        this.tenantId = tenantId;
        this.terminalId = terminalId;
        this.sessionId = sessionId;
        this.drawId = drawId;
        this.ticketCode = ticketCode;
        this.publicCode = publicCode;
        this.lines = lines;
        this.totalAmount =
            lines.stream().map(TicketLine::stake).reduce(BigDecimal.ZERO, BigDecimal::add);
        this.status = TicketStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    /**
     * Factory method to ensure a Ticket is created in a valid initial state.
     */
    public static Ticket create(
        UUID tenantId,
        UUID terminalId,
        UUID sessionId,
        UUID drawId,
        String ticketCode,
        String publicCode,
        List<TicketLine> lines) {
        Objects.requireNonNull(tenantId, "TenantId cannot be null");
        Objects.requireNonNull(terminalId, "TerminalId cannot be null");
        Objects.requireNonNull(drawId, "DrawId cannot be null");
        Objects.requireNonNull(ticketCode, "TicketCode cannot be null");
        Objects.requireNonNull(publicCode, "PublicCode cannot be null");
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("A ticket must have at least one line.");
        }
        return new Ticket(
            UUID.randomUUID(), tenantId, terminalId, sessionId, drawId, ticketCode, publicCode, lines);
    }

    // --- Business Methods ---

    public void markAsPaid() {
        if (this.status != TicketStatus.WON) {
            throw new IllegalStateException(
                "Only a WON ticket can be marked as PAID. Current status: " + this.status);
        }
        this.status = TicketStatus.PAID;
        this.updatedAt = Instant.now();
    }

    public void voidTicket() {
        if (this.status != TicketStatus.PENDING) {
            throw new IllegalStateException(
                "Only a PENDING ticket can be voided. Current status: " + this.status);
        }
        this.status = TicketStatus.VOID;
        this.updatedAt = Instant.now();
    }

    // --- Getters ---

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getTerminalId() {
        return terminalId;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public UUID getDrawId() {
        return drawId;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public String getPublicCode() {
        return publicCode;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<TicketLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public BigDecimal getTotalStake() {
        return totalStake;
    }

    public void setTotalStake(BigDecimal totalStake) {
        this.totalStake = totalStake;
    }

    public BigDecimal getPotentialPayout() {
        return potentialPayout;
    }

    public void setPotentialPayout(BigDecimal potentialPayout) {
        this.potentialPayout = potentialPayout;
    }

    public String getOutletName() {
        return outletName;
    }

    public void setOutletName(String outletName) {
        this.outletName = outletName;
    }

    public String getGameCode() {
        return gameCode;
    }

    public void setGameCode(String gameCode) {
        this.gameCode = gameCode;
    }

    public String getDrawCode() {
        return drawCode;
    }

    public void setDrawCode(String drawCode) {
        this.drawCode = drawCode;
    }

    public Instant getDrawDateTime() {
        return drawDateTime;
    }

    public void setDrawDateTime(Instant drawDateTime) {
        this.drawDateTime = drawDateTime;
    }
}
