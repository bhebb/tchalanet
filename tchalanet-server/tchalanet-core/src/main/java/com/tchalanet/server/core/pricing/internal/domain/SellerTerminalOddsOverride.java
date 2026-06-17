package com.tchalanet.server.core.pricing.internal.domain;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.SellerTerminalOddsOverrideId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

import java.math.BigDecimal;
import java.time.Instant;

public record SellerTerminalOddsOverride(
    SellerTerminalOddsOverrideId id,
    TenantId tenantId,
    SellerTerminalId sellerTerminalId,
    String gameCode,
    String betType,
    Short betOption,
    BigDecimal odds,
    boolean active,
    Instant effectiveFrom,
    Instant effectiveTo,
    String reason,
    Instant createdAt,
    UserId createdBy,
    Instant updatedAt,
    UserId updatedBy,
    Instant deletedAt
) {

    public static SellerTerminalOddsOverride createNew(
        SellerTerminalOddsOverrideId id,
        TenantId tenantId,
        SellerTerminalId sellerTerminalId,
        String gameCode,
        String betType,
        Short betOption,
        BigDecimal odds,
        Instant effectiveFrom,
        Instant effectiveTo,
        String reason,
        UserId actorId
    ) {
        var now = Instant.now();
        return new SellerTerminalOddsOverride(
            id, tenantId, sellerTerminalId,
            gameCode, betType, betOption,
            odds, true,
            effectiveFrom, effectiveTo, reason,
            now, actorId, now, actorId, null);
    }

    public SellerTerminalOddsOverride update(
        BigDecimal newOdds,
        Instant effectiveFrom,
        Instant effectiveTo,
        String reason,
        UserId actorId
    ) {
        return new SellerTerminalOddsOverride(
            id, tenantId, sellerTerminalId,
            gameCode, betType, betOption,
            newOdds, true,
            effectiveFrom, effectiveTo, reason,
            createdAt, createdBy, Instant.now(), actorId, null);
    }

    public SellerTerminalOddsOverride deactivate(UserId actorId) {
        return new SellerTerminalOddsOverride(
            id, tenantId, sellerTerminalId,
            gameCode, betType, betOption,
            odds, false,
            effectiveFrom, effectiveTo, reason,
            createdAt, createdBy, Instant.now(), actorId, null);
    }

    public SellerTerminalOddsOverride softDelete(UserId actorId) {
        return new SellerTerminalOddsOverride(
            id, tenantId, sellerTerminalId,
            gameCode, betType, betOption,
            odds, false,
            effectiveFrom, effectiveTo, reason,
            createdAt, createdBy, Instant.now(), actorId, Instant.now());
    }
}
