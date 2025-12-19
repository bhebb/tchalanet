package com.tchalanet.server.core.session.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record PosSession(
    UUID id,
    UUID tenantId,
    UUID outletId,
    UUID terminalId,
    UUID userId,
    PosSessionStatus status,
    Instant openedAt,
    Instant closedAt,
    long openingFloatCents,    // nullable côté DB, mais ici on met 0 par défaut
    long closingAmountCents,   // idem
    long totalTickets,
    long totalStakeCents,
    long totalPayoutCents,
    long grossMarginCents,
    Map<String, Object> meta,
    long version
) {

    public static PosSession open(
        UUID id,
        UUID tenantId,
        UUID outletId,
        UUID terminalId,
        UUID userId,
        Long openingFloatCents,  // peut être null côté commande
        Instant now
    ) {
        Objects.requireNonNull(tenantId, "tenantId is required");
        Objects.requireNonNull(outletId, "outletId is required");
        Objects.requireNonNull(terminalId, "terminalId is required");
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(now, "now is required");

        long opening = openingFloatCents != null ? openingFloatCents : 0L;

        return new PosSession(
            id,
            tenantId,
            outletId,
            terminalId,
            userId,
            PosSessionStatus.OPENED,
            now,
            null,
            opening,
            0L,
            0L,
            0L,
            0L,
            0L,
            Map.of(),
            0L
        );
    }

    public PosSession close(long closingAmountCents, Instant now) {
        if (status != PosSessionStatus.OPENED) {
            throw new IllegalStateException(
                "Only an OPEN session can be closed. Current status: " + status);
        }
        Objects.requireNonNull(now, "now is required");

        return new PosSession(
            id,
            tenantId,
            outletId,
            terminalId,
            userId,
            PosSessionStatus.CLOSED,
            openedAt,
            now,
            openingFloatCents,
            closingAmountCents,
            totalTickets,
            totalStakeCents,
            totalPayoutCents,
            grossMarginCents,
            meta,
            version
        );
    }

    public PosSession withTotals(long totalTickets,
                                 long totalStakeCents,
                                 long totalPayoutCents,
                                 long grossMarginCents) {
        return new PosSession(
            id,
            tenantId,
            outletId,
            terminalId,
            userId,
            status,
            openedAt,
            closedAt,
            openingFloatCents,
            closingAmountCents,
            totalTickets,
            totalStakeCents,
            totalPayoutCents,
            grossMarginCents,
            meta,
            version
        );
    }
}
