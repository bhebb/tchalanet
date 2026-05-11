package com.tchalanet.server.core.session.domain.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;
import java.time.LocalDate;

public record SalesSession(
    SalesSessionId id,
    TenantId tenantId,
    OutletId outletId,
    TerminalId terminalId,
    UserId openedBy,
    Instant openedAt,
    LocalDate businessDate,
    SalesSessionStatus status,
    UserId closedBy,
    Instant closedAt,
    String closeReason,
    Long openingFloatCents,
    Long expectedClosingAmountCents,
    Long declaredClosingAmountCents,
    Long varianceCents) {

    public static SalesSession open(
        SalesSessionId id,
        TenantId tenantId,
        OutletId outletId,
        TerminalId terminalId,
        UserId openedBy,
        LocalDate businessDate,
        Instant openedAt,
        Long openingFloatCents) {

        return new SalesSession(
            id,
            tenantId,
            outletId,
            terminalId,
            openedBy,
            openedAt,
            businessDate,
            SalesSessionStatus.OPEN,
            null,
            null,
            null,
            openingFloatCents,
            null,
            null,
            null);
    }

    public static SalesSession load(
        SalesSessionId id,
        TenantId tenantId,
        OutletId outletId,
        TerminalId terminalId,
        UserId openedBy,
        Instant openedAt,
        LocalDate businessDate,
        SalesSessionStatus status,
        UserId closedBy,
        Instant closedAt,
        String closeReason,
        Long openingFloatCents,
        Long expectedClosingAmountCents,
        Long declaredClosingAmountCents,
        Long varianceCents) {

        return new SalesSession(
            id,
            tenantId,
            outletId,
            terminalId,
            openedBy,
            openedAt,
            businessDate,
            status,
            closedBy,
            closedAt,
            closeReason,
            openingFloatCents,
            expectedClosingAmountCents,
            declaredClosingAmountCents,
            varianceCents);
    }

    public SalesSession close(
        UserId closedBy,
        Instant closedAt,
        SalesSessionCashSummary cashSummary,
        String reason) {

        if (status != SalesSessionStatus.OPEN) {
            throw new IllegalStateException("Session is not open");
        }

        return new SalesSession(
            id,
            tenantId,
            outletId,
            terminalId,
            openedBy,
            openedAt,
            businessDate,
            SalesSessionStatus.CLOSED,
            closedBy,
            closedAt,
            reason,
            openingFloatCents,
            cashSummary.expectedClosingAmountCents(),
            cashSummary.declaredClosingAmountCents(),
            cashSummary.varianceCents());
    }

    public boolean isOpen() {
        return status == SalesSessionStatus.OPEN;
    }

    public boolean isClosed() {
        return status == SalesSessionStatus.CLOSED;
    }
}
