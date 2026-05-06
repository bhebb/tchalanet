package com.tchalanet.server.core.sales.domain.model;

import com.tchalanet.server.common.types.enums.TicketResultStatus;
import com.tchalanet.server.common.types.enums.TicketSaleStatus;
import com.tchalanet.server.common.types.enums.TicketSettlementStatus;
import com.tchalanet.server.common.types.id.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
public class Ticket {

    private final TicketId id;
    private final TenantId tenantId;
    private final TerminalId terminalId;
    private final SessionId sessionId; // nullable (backfills)
    private final DrawId drawId;

    private final String ticketCode; // per-tenant unique
    private final String publicCode; // globally unique

    private final String currency;

    private TicketSaleStatus saleStatus;
    private TicketResultStatus resultStatus;
    private TicketSettlementStatus settlementStatus;

    private final BigDecimal totalAmount;

    private final Instant createdAt;
    private Instant updatedAt;

    // nullable until resulted
    private BigDecimal winningAmount; // null when NOT_RESULTED
    private Instant resultedAt;       // null when NOT_RESULTED

    private final UUID approvalRequestId; // nullable; present when PENDING_APPROVAL

    private final List<TicketLine> lines;

    private Ticket(
        TicketId id,
        TenantId tenantId,
        TerminalId terminalId,
        SessionId sessionId,
        DrawId drawId,
        String ticketCode,
        String publicCode,
        String currency,
        TicketSaleStatus saleStatus,
        TicketResultStatus resultStatus,
        TicketSettlementStatus settlementStatus,
        BigDecimal totalAmount,
        BigDecimal winningAmount,
        Instant resultedAt,
        List<TicketLine> lines,
        UUID approvalRequestId,
        Instant createdAt,
        Instant updatedAt) {

        this.id = Objects.requireNonNull(id, "id");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.terminalId = Objects.requireNonNull(terminalId, "terminalId");
        this.sessionId = sessionId;
        this.drawId = Objects.requireNonNull(drawId, "drawId");

        this.ticketCode = requireNonBlank(ticketCode, "ticketCode");
        this.publicCode = requireNonBlank(publicCode, "publicCode");

        this.currency = requireNonBlank(currency, "currency");

        this.saleStatus = Objects.requireNonNull(saleStatus, "saleStatus");
        this.resultStatus = Objects.requireNonNull(resultStatus, "resultStatus");
        this.settlementStatus = Objects.requireNonNull(settlementStatus, "settlementStatus");

        this.lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
        if (this.lines.isEmpty()) throw new IllegalArgumentException("ticket must have >= 1 line");

        this.totalAmount = requireNonNeg(Objects.requireNonNull(totalAmount, "totalAmount"), "totalAmount");

        this.approvalRequestId = approvalRequestId; // nullable

        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");

        // --- normalize/enforce result invariants
        if (this.resultStatus == TicketResultStatus.NOT_RESULTED) {
            if (winningAmount != null || resultedAt != null) {
                throw new IllegalArgumentException("Result fields must be null when NOT_RESULTED");
            }
            this.winningAmount = null;
            this.resultedAt = null;
            if (this.settlementStatus == TicketSettlementStatus.SETTLED) {
                throw new IllegalArgumentException("Cannot be SETTLED when NOT_RESULTED");
            }
        } else {
            this.winningAmount = requireNonNeg(
                Objects.requireNonNull(winningAmount, "winningAmount"), "winningAmount");
            this.resultedAt = Objects.requireNonNull(resultedAt, "resultedAt");
        }
    }

    // ---- Factory: SOLD directly
    public static Ticket sell(
        TicketId id,
        TenantId tenantId,
        TerminalId terminalId,
        SessionId sessionId,
        DrawId drawId,
        String ticketCode,
        String publicCode,
        Currency currency,
        List<TicketLine> lines,
        Instant now) {

        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(lines, "lines");
        BigDecimal total = lines.stream().map(TicketLine::stake).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new Ticket(
            id,
            tenantId,
            terminalId,
            sessionId,
            drawId,
            ticketCode,
            publicCode,
            currency.getCurrencyCode(),
            TicketSaleStatus.SOLD,
            TicketResultStatus.NOT_RESULTED,
            TicketSettlementStatus.UNSETTLED,
            total,
            null,
            null,
            lines,
            null,
            now,
            now);
    }

    // ---- Factory: PENDING_APPROVAL with approval request id
    public static Ticket requestApproval(
        TicketId id,
        TenantId tenantId,
        TerminalId terminalId,
        SessionId sessionId,
        DrawId drawId,
        String ticketCode,
        String publicCode,
        String currency,
        List<TicketLine> lines,
        Instant now,
        UUID approvalRequestId) {

        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(approvalRequestId, "approvalRequestId");
        Objects.requireNonNull(lines, "lines");
        BigDecimal total = lines.stream().map(TicketLine::stake).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new Ticket(
            id,
            tenantId,
            terminalId,
            sessionId,
            drawId,
            ticketCode,
            publicCode,
            currency,
            TicketSaleStatus.PENDING_APPROVAL,
            TicketResultStatus.NOT_RESULTED,
            TicketSettlementStatus.UNSETTLED,
            total,
            null,
            null,
            lines,
            approvalRequestId,
            now,
            now);
    }

    // ---- Rehydrate
    public static Ticket rehydrate(
        TicketId id,
        TenantId tenantId,
        TerminalId terminalId,
        SessionId sessionId,
        DrawId drawId,
        String ticketCode,
        String publicCode,
        String currency,
        TicketSaleStatus saleStatus,
        TicketResultStatus resultStatus,
        TicketSettlementStatus settlementStatus,
        BigDecimal totalAmount,
        BigDecimal winningAmount,
        Instant resultedAt,
        List<TicketLine> lines,
        UUID approvalRequestId,
        Instant createdAt,
        Instant updatedAt) {

        return new Ticket(
            id,
            tenantId,
            terminalId,
            sessionId,
            drawId,
            ticketCode,
            publicCode,
            currency,
            saleStatus,
            resultStatus,
            settlementStatus,
            totalAmount == null ? BigDecimal.ZERO : totalAmount,
            winningAmount,
            resultedAt,
            lines == null ? List.of() : lines,
            approvalRequestId,
            createdAt,
            updatedAt);
    }

    // ---- State transitions

    public void approve(Instant when) {
        requireSaleStatus(TicketSaleStatus.PENDING_APPROVAL);
        this.saleStatus = TicketSaleStatus.SOLD;
        touch(when);
    }

    public void reject(Instant when) {
        requireSaleStatus(TicketSaleStatus.PENDING_APPROVAL);
        this.saleStatus = TicketSaleStatus.REJECTED;
        touch(when);
    }

    public void voidTicket(Instant when) {
        if (saleStatus != TicketSaleStatus.SOLD && saleStatus != TicketSaleStatus.PENDING_APPROVAL) {
            throw new IllegalStateException(
                "Only SOLD or PENDING_APPROVAL can be voided. saleStatus=" + saleStatus);
        }
        this.saleStatus = TicketSaleStatus.VOID;
        touch(when);
    }

    public void markResulted(BigDecimal payout, Instant when) {
        Objects.requireNonNull(payout, "payout");
        if (payout.signum() < 0) throw new IllegalArgumentException("payout must be >= 0");
        requireSaleStatus(TicketSaleStatus.SOLD);

        this.winningAmount = payout;
        this.resultedAt = Objects.requireNonNull(when, "when");
        this.resultStatus = payout.signum() > 0 ? TicketResultStatus.WON : TicketResultStatus.LOST;

        // payout domain later; sales keeps it UNSETTLED
        this.settlementStatus = TicketSettlementStatus.UNSETTLED;
        touch(when);
    }

    public void forceResult(BigDecimal payout, Instant when) {
        Objects.requireNonNull(payout, "payout");
        if (payout.signum() < 0) throw new IllegalArgumentException("payout must be >= 0");
        if (saleStatus == TicketSaleStatus.VOID) {
            throw new IllegalStateException("Cannot override result for VOID ticket");
        }

        this.winningAmount = payout;
        this.resultedAt = Objects.requireNonNull(when, "when");
        this.resultStatus = TicketResultStatus.OVERRIDDEN;
        this.settlementStatus = TicketSettlementStatus.UNSETTLED;
        touch(when);
    }

    // New overload: allow specifying explicit resultStatus (WON or LOST) when forcing result
    public void forceResult(BigDecimal payout, TicketResultStatus resultStatus, Instant when) {
        Objects.requireNonNull(payout, "payout");
        Objects.requireNonNull(resultStatus, "resultStatus");
        if (payout.signum() < 0) throw new IllegalArgumentException("payout must be >= 0");
        if (saleStatus == TicketSaleStatus.VOID) {
            throw new IllegalStateException("Cannot override result for VOID ticket");
        }
        if (resultStatus != TicketResultStatus.WON && resultStatus != TicketResultStatus.LOST) {
            throw new IllegalArgumentException("resultStatus must be WON or LOST");
        }

        this.winningAmount = payout;
        this.resultedAt = Objects.requireNonNull(when, "when");
        this.resultStatus = resultStatus;
        this.settlementStatus = TicketSettlementStatus.UNSETTLED;
        touch(when);
    }

    public void settle(Instant when) {
        if (resultStatus == TicketResultStatus.NOT_RESULTED) {
            throw new IllegalStateException("Only resulted tickets can be settled");
        }
        this.settlementStatus = TicketSettlementStatus.SETTLED;
        touch(when);
    }

    /**
     * Return the total payout amount for this ticket (the amount that should be paid).
     * Currently this is the recorded winningAmount (zero when not won).
     */
    public java.math.BigDecimal totalPayout() {
        return this.winningAmount == null ? java.math.BigDecimal.ZERO : this.winningAmount;
    }

    private void requireSaleStatus(TicketSaleStatus expected) {
        if (this.saleStatus != expected) {
            throw new IllegalStateException("Expected " + expected + " but was " + saleStatus);
        }
    }

    private void touch(Instant when) {
        this.updatedAt = Objects.requireNonNull(when, "when");
    }

    public List<TicketLine> getLines() {
        return List.copyOf(lines);
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must be non-blank");
        return value;
    }

    private static BigDecimal requireNonNeg(BigDecimal amount, String field) {
        if (amount.signum() < 0) throw new IllegalArgumentException(field + " must be >= 0");
        return amount;
    }

    // Record-style accessors (some legacy code expects .id(), .tenantId() etc.)
    public TicketId id() { return this.id; }
    public TenantId tenantId() { return this.tenantId; }
    public TerminalId terminalId() { return this.terminalId; }
    public DrawId drawId() { return this.drawId; }
}
