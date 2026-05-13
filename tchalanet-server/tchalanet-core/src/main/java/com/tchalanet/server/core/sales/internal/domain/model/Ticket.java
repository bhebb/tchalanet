package com.tchalanet.server.core.sales.internal.domain.model;

import com.tchalanet.server.common.types.id.*;
import com.tchalanet.server.common.types.money.CurrencyCode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import lombok.Getter;

@Getter
public class Ticket {
    private final TicketId id;
    private final TenantId tenantId;
    private final OutletId outletId;
    private final TerminalId terminalId;
    private final UserId sellerUserId;
    private final SalesSessionId salesSessionId;
    private final DrawId drawId;
    private final DrawChannelId drawChannelId;
    private final String ticketCode;
    private final String publicCode;
    private final String verificationCode;
    private final CurrencyCode currency;
    private final TicketMoneyBreakdown money;
    private final BigDecimal potentialPayoutAmount;
    private BigDecimal winningAmount;
    private com.tchalanet.server.common.types.enums.TicketSaleStatus saleStatus;
    private com.tchalanet.server.common.types.enums.TicketResultStatus resultStatus;
    private com.tchalanet.server.common.types.enums.TicketSettlementStatus settlementStatus;
    private final com.tchalanet.server.common.types.enums.SaleOrigin saleOrigin;
    private final com.tchalanet.server.common.types.enums.TicketSyncStatus syncStatus;
    private final OfflineSaleRef offlineSaleRef;
    private final SalesSessionPostingMode sessionPostingMode;
    private final Instant soldAt;
    private Instant resultedAt;
    private Instant settledAt;
    private Instant paidAt;
    private UserId paidBy;
    private final List<TicketLine> lines;
    private final Instant createdAt;
    private Instant updatedAt;
    private final ApprovalRequestId approvalRequestId;

    private Ticket(
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
        com.tchalanet.server.common.types.enums.TicketSaleStatus saleStatus,
        com.tchalanet.server.common.types.enums.TicketResultStatus resultStatus,
        com.tchalanet.server.common.types.enums.TicketSettlementStatus settlementStatus,
        com.tchalanet.server.common.types.enums.SaleOrigin saleOrigin,
        com.tchalanet.server.common.types.enums.TicketSyncStatus syncStatus,
        OfflineSaleRef offlineSaleRef,
        SalesSessionPostingMode sessionPostingMode,
        Instant soldAt,
        Instant resultedAt,
        Instant settledAt,
        Instant paidAt,
        UserId paidBy,
        List<TicketLine> lines,
        Instant createdAt,
        Instant updatedAt,
        ApprovalRequestId approvalRequestId
    ) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId is required");
        this.outletId = Objects.requireNonNull(outletId, "outletId is required");
        this.terminalId = Objects.requireNonNull(terminalId, "terminalId is required");
        this.sellerUserId = Objects.requireNonNull(sellerUserId, "sellerUserId is required");
        this.salesSessionId = Objects.requireNonNull(salesSessionId, "salesSessionId is required");
        this.drawId = Objects.requireNonNull(drawId, "drawId is required");
        this.drawChannelId = drawChannelId;
        this.ticketCode = Objects.requireNonNull(ticketCode, "ticketCode is required");
        this.publicCode = Objects.requireNonNull(publicCode, "publicCode is required");
        this.verificationCode = Objects.requireNonNull(verificationCode, "verificationCode is required");
        this.currency = Objects.requireNonNull(currency, "currency is required");
        this.money = Objects.requireNonNull(money, "money is required");
        this.potentialPayoutAmount = Objects.requireNonNull(potentialPayoutAmount, "potentialPayoutAmount is required");
        this.winningAmount = winningAmount;
        this.saleStatus = Objects.requireNonNull(saleStatus, "saleStatus is required");
        this.resultStatus = Objects.requireNonNull(resultStatus, "resultStatus is required");
        this.settlementStatus = Objects.requireNonNull(settlementStatus, "settlementStatus is required");
        this.saleOrigin = Objects.requireNonNull(saleOrigin, "saleOrigin is required");
        this.syncStatus = Objects.requireNonNull(syncStatus, "syncStatus is required");
        this.offlineSaleRef = offlineSaleRef;
        this.sessionPostingMode = Objects.requireNonNull(sessionPostingMode, "sessionPostingMode is required");
        this.soldAt = Objects.requireNonNull(soldAt, "soldAt is required");
        this.resultedAt = resultedAt;
        this.settledAt = settledAt;
        this.paidAt = paidAt;
        this.paidBy = paidBy;
        this.lines = List.copyOf(lines == null ? List.of() : lines);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.approvalRequestId = approvalRequestId;

        if (this.lines.isEmpty()) throw new IllegalArgumentException("lines is required");
        if (saleOrigin == com.tchalanet.server.common.types.enums.SaleOrigin.OFFLINE && offlineSaleRef == null) throw new IllegalArgumentException("offlineSaleRef required for offline ticket");
        if (saleOrigin == com.tchalanet.server.common.types.enums.SaleOrigin.ONLINE && offlineSaleRef != null) throw new IllegalArgumentException("offlineSaleRef forbidden for online ticket");
    }

    public Ticket(
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
        com.tchalanet.server.common.types.enums.TicketSaleStatus saleStatus,
        com.tchalanet.server.common.types.enums.TicketResultStatus resultStatus,
        com.tchalanet.server.common.types.enums.TicketSettlementStatus settlementStatus,
        com.tchalanet.server.common.types.enums.SaleOrigin saleOrigin,
        com.tchalanet.server.common.types.enums.TicketSyncStatus syncStatus,
        OfflineSaleRef offlineSaleRef,
        SalesSessionPostingMode sessionPostingMode,
        Instant soldAt,
        Instant resultedAt,
        Instant settledAt,
        Instant paidAt,
        UserId paidBy,
        List<TicketLine> lines) {
        this(
            id, tenantId, outletId, terminalId, sellerUserId, salesSessionId, drawId, drawChannelId,
            ticketCode, publicCode, verificationCode, currency, money, potentialPayoutAmount,
            winningAmount, saleStatus, resultStatus, settlementStatus, saleOrigin, syncStatus,
            offlineSaleRef, sessionPostingMode, soldAt, resultedAt, settledAt, paidAt, paidBy,
            lines, soldAt, null, null);
    }

  public boolean paidOut() {
    return settlementStatus == com.tchalanet.server.common.types.enums.TicketSettlementStatus.SETTLED || paidAt != null;
  }

  public void markPaid(UserId paidBy, Instant paidAt) {
    if (paidOut()) return;
    this.paidBy = paidBy;
    this.paidAt = paidAt;
    this.settlementStatus = com.tchalanet.server.common.types.enums.TicketSettlementStatus.SETTLED;
  }

    public static Ticket sell(
        TicketId id,
        TenantId tenantId,
        TerminalId terminalId,
        SalesSessionId salesSessionId,
        DrawId drawId,
        String ticketCode,
        String publicCode,
        java.util.Currency currency,
        List<TicketLine> lines,
        Instant soldAt) {
        var stake = lines.stream()
            .map(TicketLine::stakeAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        var potential = lines.stream()
            .map(TicketLine::potentialPayoutAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Ticket(
            id,
            tenantId,
            OutletId.of(tenantId.value()),
            terminalId,
            UserId.of(tenantId.value()),
            salesSessionId,
            drawId,
            null,
            ticketCode,
            publicCode,
            publicCode,
            CurrencyCode.of(currency.getCurrencyCode()),
            new TicketMoneyBreakdown(stake, BigDecimal.ZERO, stake),
            potential,
            null,
            com.tchalanet.server.common.types.enums.TicketSaleStatus.SOLD,
            com.tchalanet.server.common.types.enums.TicketResultStatus.NOT_RESULTED,
            com.tchalanet.server.common.types.enums.TicketSettlementStatus.UNSETTLED,
            com.tchalanet.server.common.types.enums.SaleOrigin.ONLINE,
            com.tchalanet.server.common.types.enums.TicketSyncStatus.NONE,
            null,
            SalesSessionPostingMode.NORMAL_OPEN_SESSION,
            soldAt,
            null,
            null,
            null,
            null,
            lines);
    }

    public static Ticket requestApproval(
        TicketId id,
        TenantId tenantId,
        TerminalId terminalId,
        SalesSessionId salesSessionId,
        DrawId drawId,
        String ticketCode,
        String publicCode,
        String currency,
        List<TicketLine> lines,
        Instant soldAt,
        ApprovalRequestId approvalRequestId) {
        var stake = lines.stream()
            .map(TicketLine::stakeAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        var potential = lines.stream()
            .map(TicketLine::potentialPayoutAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Ticket(
            id,
            tenantId,
            OutletId.of(tenantId.value()),
            terminalId,
            UserId.of(tenantId.value()),
            salesSessionId,
            drawId,
            null,
            ticketCode,
            publicCode,
            publicCode,
            CurrencyCode.of(currency),
            new TicketMoneyBreakdown(stake, BigDecimal.ZERO, stake),
            potential,
            null,
            com.tchalanet.server.common.types.enums.TicketSaleStatus.PENDING_APPROVAL,
            com.tchalanet.server.common.types.enums.TicketResultStatus.NOT_RESULTED,
            com.tchalanet.server.common.types.enums.TicketSettlementStatus.UNSETTLED,
            com.tchalanet.server.common.types.enums.SaleOrigin.ONLINE,
            com.tchalanet.server.common.types.enums.TicketSyncStatus.NONE,
            null,
            SalesSessionPostingMode.NORMAL_OPEN_SESSION,
            soldAt,
            null,
            null,
            null,
            null,
            lines,
            soldAt,
            null,
            approvalRequestId);
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

        // Note: Rehydrate might need more fields if the domain grows,
        // but let's keep it consistent with what Mapper uses.
        // We might need to adjust Mapper to provide all fields.

        return new Ticket(
            id,
            tenantId,
            OutletId.of(tenantId.value()), // Mock/Default if missing in rehydrate
            terminalId,
            UserId.of(tenantId.value()), // Mock/Default
            sessionId,
            drawId,
            null,
            ticketCode,
            publicCode,
            "REHYDRATED",
            CurrencyCode.of(currency),
            new TicketMoneyBreakdown(totalAmount, BigDecimal.ZERO, totalAmount),
            BigDecimal.ZERO,
            winningAmount,
            saleStatus,
            resultStatus,
            settlementStatus,
            com.tchalanet.server.common.types.enums.SaleOrigin.ONLINE,
            com.tchalanet.server.common.types.enums.TicketSyncStatus.NONE,
            null,
            SalesSessionPostingMode.NORMAL_OPEN_SESSION,
            createdAt != null ? createdAt : Instant.now(),
            resultedAt,
            null,
            null,
            null,
            lines,
            createdAt,
            updatedAt,
            approvalRequestId);
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

    public TicketId id() {
        return id;
    }

    public String publicCode() {
        return publicCode;
    }

    public com.tchalanet.server.common.types.enums.TicketSaleStatus saleStatus() {
        return saleStatus;
    }

    public com.tchalanet.server.common.types.enums.TicketResultStatus resultStatus() {
        return resultStatus;
    }

    public com.tchalanet.server.common.types.enums.TicketSettlementStatus settlementStatus() {
        return settlementStatus;
    }

    public com.tchalanet.server.common.types.enums.SaleOrigin saleOrigin() {
        return saleOrigin;
    }

    public com.tchalanet.server.common.types.enums.TicketSyncStatus syncStatus() {
        return syncStatus;
    }

    public TicketMoneyBreakdown money() {
        return money;
    }

    public String ticketCode() {
        return ticketCode;
    }

    public Instant soldAt() {
        return soldAt;
    }

    public SalesSessionId getSessionId() {
        return salesSessionId;
    }

    public BigDecimal getTotalAmount() {
        return money.totalAmount();
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
