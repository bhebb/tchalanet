package com.tchalanet.server.core.sales.internal.domain.model;

import com.tchalanet.server.common.types.id.*;
import com.tchalanet.server.common.types.money.CurrencyCode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Objects;

public record Ticket(
    TicketId id,
    TenantId tenantId,
    OutletId outletId,
    TerminalId terminalId,
    UserId sellerUserId,
    SalesSessionId salesSessionId,
    DrawId drawId,
    DrawChannelId drawChannelId,
    String ticketCode,
    String publicCode,
    String verificationCode,
    CurrencyCode currency,
    TicketMoneyBreakdown money,
    BigDecimal potentialPayoutAmount,
    BigDecimal winningAmount,
    TicketSaleStatus saleStatus,
    TicketResultStatus resultStatus,
    TicketSettlementStatus settlementStatus,
    SaleOrigin saleOrigin,
    TicketSyncStatus syncStatus,
    OfflineSaleRef offlineSaleRef,
    SalesSessionPostingMode sessionPostingMode,
    Instant soldAt,
    Instant resultedAt,
    Instant settledAt,
    Instant paidAt,
    UserId paidBy,
    List<TicketLine> lines
) {
  public Ticket {
    Objects.requireNonNull(id, "id is required");
    Objects.requireNonNull(tenantId, "tenantId is required");
    Objects.requireNonNull(outletId, "outletId is required");
    Objects.requireNonNull(terminalId, "id is required");
    Objects.requireNonNull(sellerUserId, "sellerUserId is required");
    Objects.requireNonNull(salesSessionId, "salesSessionId is required");
    Objects.requireNonNull(drawId, "drawId is required");
    Objects.requireNonNull(ticketCode, "ticketCode is required");
    Objects.requireNonNull(publicCode, "publicCode is required");
    Objects.requireNonNull(verificationCode, "verificationCode is required");
    Objects.requireNonNull(currency, "currency is required");
    Objects.requireNonNull(money, "money is required");
    Objects.requireNonNull(potentialPayoutAmount, "potentialPayoutAmount is required");
    Objects.requireNonNull(saleStatus, "saleStatus is required");
    Objects.requireNonNull(resultStatus, "resultStatus is required");
    Objects.requireNonNull(settlementStatus, "settlementStatus is required");
    Objects.requireNonNull(saleOrigin, "saleOrigin is required");
    Objects.requireNonNull(syncStatus, "syncStatus is required");
    Objects.requireNonNull(sessionPostingMode, "sessionPostingMode is required");
    Objects.requireNonNull(soldAt, "soldAt is required");
    lines = List.copyOf(lines == null ? List.of() : lines);
    if (lines.isEmpty()) throw new IllegalArgumentException("lines is required");
    if (saleOrigin == SaleOrigin.OFFLINE && offlineSaleRef == null) throw new IllegalArgumentException("offlineSaleRef required for offline ticket");
    if (saleOrigin == SaleOrigin.ONLINE && offlineSaleRef != null) throw new IllegalArgumentException("offlineSaleRef forbidden for online ticket");
  }

  public boolean paidOut() {
    return settlementStatus == TicketSettlementStatus.PAID_OUT || paidAt != null;
  }

  public Ticket markPaid(UserId paidBy, Instant paidAt) {
    if (paidOut()) return this;
    return new Ticket(id, tenantId, outletId, terminalId, sellerUserId, salesSessionId, drawId, drawChannelId,
        ticketCode, publicCode, verificationCode, currency, money, potentialPayoutAmount, winningAmount,
        saleStatus, resultStatus, TicketSettlementStatus.PAID_OUT, saleOrigin, syncStatus, offlineSaleRef,
        sessionPostingMode, soldAt, resultedAt, settledAt, paidAt, paidBy, lines);
  }

    public static Ticket sell(
        TicketId id,
        TenantId tenantId,
        TerminalId terminalId,
        SalesSessionId sessionId,
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
            com.tchalanet.server.common.types.enums.TicketSaleStatus.SOLD,
            com.tchalanet.server.common.types.enums.TicketResultStatus.NOT_RESULTED,
            com.tchalanet.server.common.types.enums.TicketSettlementStatus.UNSETTLED,
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
        SalesSessionId sessionId,
        DrawId drawId,
        String ticketCode,
        String publicCode,
        String currency,
        List<TicketLine> lines,
        Instant now,
        ApprovalRequestId approvalRequestId) {

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
            com.tchalanet.server.common.types.enums.TicketSaleStatus.PENDING_APPROVAL,
            com.tchalanet.server.common.types.enums.TicketResultStatus.NOT_RESULTED,
            com.tchalanet.server.common.types.enums.TicketSettlementStatus.UNSETTLED,
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
        SalesSessionId sessionId,
        DrawId drawId,
        String ticketCode,
        String publicCode,
        String currency,
        com.tchalanet.server.common.types.enums.TicketSaleStatus saleStatus,
        com.tchalanet.server.common.types.enums.TicketResultStatus resultStatus,
        com.tchalanet.server.common.types.enums.TicketSettlementStatus settlementStatus,
        BigDecimal totalAmount,
        BigDecimal winningAmount,
        Instant resultedAt,
        List<TicketLine> lines,
        ApprovalRequestId approvalRequestId,
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
        requireSaleStatus(com.tchalanet.server.common.types.enums.TicketSaleStatus.PENDING_APPROVAL);
        this.saleStatus = com.tchalanet.server.common.types.enums.TicketSaleStatus.SOLD;
        touch(when);
    }

    public void reject(Instant when) {
        requireSaleStatus(com.tchalanet.server.common.types.enums.TicketSaleStatus.PENDING_APPROVAL);
        this.saleStatus = com.tchalanet.server.common.types.enums.TicketSaleStatus.REJECTED;
        touch(when);
    }

    public void voidTicket(Instant when) {
        if (saleStatus != com.tchalanet.server.common.types.enums.TicketSaleStatus.SOLD && saleStatus != com.tchalanet.server.common.types.enums.TicketSaleStatus.PENDING_APPROVAL) {
            throw new IllegalStateException(
                "Only SOLD or PENDING_APPROVAL can be voided. saleStatus=" + saleStatus);
        }
        this.saleStatus = com.tchalanet.server.common.types.enums.TicketSaleStatus.VOID;
        touch(when);
    }

    public void markResulted(BigDecimal payout, Instant when) {
        Objects.requireNonNull(payout, "payout");
        if (payout.signum() < 0) throw new IllegalArgumentException("payout must be >= 0");
        requireSaleStatus(com.tchalanet.server.common.types.enums.TicketSaleStatus.SOLD);

        this.winningAmount = payout;
        this.resultedAt = Objects.requireNonNull(when, "when");
        this.resultStatus = payout.signum() > 0 ? com.tchalanet.server.common.types.enums.TicketResultStatus.WON : com.tchalanet.server.common.types.enums.TicketResultStatus.LOST;

        // payout domain later; sales keeps it UNSETTLED
        this.settlementStatus = com.tchalanet.server.common.types.enums.TicketSettlementStatus.UNSETTLED;
        touch(when);
    }

    public void overrideAsResulted(BigDecimal payout, Instant when) {
        Objects.requireNonNull(payout, "payout");
        if (payout.signum() < 0) throw new IllegalArgumentException("payout must be >= 0");
        if (saleStatus == com.tchalanet.server.common.types.enums.TicketSaleStatus.VOID) {
            throw new IllegalStateException("Cannot override result for VOID ticket");
        }

        this.winningAmount = payout;
        this.resultedAt = Objects.requireNonNull(when, "when");
        this.resultStatus = com.tchalanet.server.common.types.enums.TicketResultStatus.OVERRIDDEN;
        this.settlementStatus = com.tchalanet.server.common.types.enums.TicketSettlementStatus.UNSETTLED;
        touch(when);
    }

    // New overload: allow specifying explicit resultStatus (WON or LOST) when forcing result
    public void forceResult(BigDecimal payout, com.tchalanet.server.common.types.enums.TicketResultStatus resultStatus, Instant when) {
        Objects.requireNonNull(payout, "payout");
        Objects.requireNonNull(resultStatus, "resultStatus");
        if (payout.signum() < 0) throw new IllegalArgumentException("payout must be >= 0");
        if (saleStatus == com.tchalanet.server.common.types.enums.TicketSaleStatus.VOID) {
            throw new IllegalStateException("Cannot override result for VOID ticket");
        }
        if (resultStatus != com.tchalanet.server.common.types.enums.TicketResultStatus.WON && resultStatus != com.tchalanet.server.common.types.enums.TicketResultStatus.LOST) {
            throw new IllegalArgumentException("resultStatus must be WON or LOST");
        }

        this.winningAmount = payout;
        this.resultedAt = Objects.requireNonNull(when, "when");
        this.resultStatus = resultStatus;
        this.settlementStatus = com.tchalanet.server.common.types.enums.TicketSettlementStatus.UNSETTLED;
        touch(when);
    }

    public void settle(Instant when) {
        if (resultStatus == com.tchalanet.server.common.types.enums.TicketResultStatus.NOT_RESULTED) {
            throw new IllegalStateException("Only resulted tickets can be settled");
        }
        this.settlementStatus = com.tchalanet.server.common.types.enums.TicketSettlementStatus.SETTLED;
        touch(when);
    }

    /**
     * Return the total payout amount for this ticket (the amount that should be paid).
     * Currently this is the recorded winningAmount (zero when not won).
     */
    public java.math.BigDecimal totalPayout() {
        return this.winningAmount == null ? java.math.BigDecimal.ZERO : this.winningAmount;
    }

    private void requireSaleStatus(com.tchalanet.server.common.types.enums.TicketSaleStatus expected) {
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
}
