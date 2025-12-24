package com.tchalanet.server.core.session.domain.model;

import java.math.BigDecimal;
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
    Long openingFloatCents,   // nullable
    Long closingAmountCents,  // nullable
    BigDecimal totalStake,
    Long totalTickets,
    BigDecimal totalPayout,
    Map<String, Object> meta,
    long version
) {

    public static PosSession open(
        UUID id,
        UUID tenantId,
        UUID outletId,
        UUID terminalId,
        UUID userId,
        Long openingFloatCents,
        Instant now
    ) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(outletId);
        Objects.requireNonNull(terminalId);
        Objects.requireNonNull(userId);
        Objects.requireNonNull(now);

        return new PosSession(
            id,
            tenantId,
            outletId,
            terminalId,
            userId,
            PosSessionStatus.OPENED,
            now,
            null,
            openingFloatCents,
            null,
            BigDecimal.ZERO,
            0L,
            BigDecimal.ZERO,
            Map.of(),
            0L
        );
    }

    public PosSession close(BigDecimal closingAmountCents, Instant now) {
        if (status != PosSessionStatus.OPENED) {
            throw new IllegalStateException("Only OPEN session can be closed. status=" + status);
        }
        Objects.requireNonNull(now);
        return new PosSession(
            id, tenantId, outletId, terminalId, userId,
            PosSessionStatus.CLOSED,
            openedAt,
            now,
            openingFloatCents,
            closingAmountCents.longValue(),
            totalStake,
            totalTickets,
            totalPayout,
            meta,
            version
        );
    }

    public static PosSession reconstruct(
        UUID id,
        UUID tenantId,
        UUID outletId,
        UUID terminalId,
        UUID userId,
        PosSessionStatus status,
        Instant openedAt,
        Instant closedAt,
        Long openingFloatCents,
        Long closingAmountCents,
        BigDecimal totalStake,
        Long totalTickets,
        BigDecimal totalPayout,
        Map<String, Object> meta,
        long version
    ) {
        return new PosSession(
            id, tenantId, outletId, terminalId, userId, status, openedAt, closedAt,
            openingFloatCents, closingAmountCents, totalStake, totalTickets, totalPayout, meta, version
        );
    }
}
