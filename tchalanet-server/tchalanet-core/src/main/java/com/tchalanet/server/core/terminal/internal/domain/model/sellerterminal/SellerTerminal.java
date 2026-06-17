package com.tchalanet.server.core.terminal.internal.domain.model.sellerterminal;

import com.tchalanet.server.common.types.id.AddressId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.terminal.api.model.SellerTerminalStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record SellerTerminal(
    SellerTerminalId id,
    TenantId tenantId,
    String terminalCode,
    String firstName,
    String lastName,
    String displayName,
    String phoneNumber,
    AddressId addressId,
    SellerTerminalStatus status,
    BigDecimal commissionRate,
    OutletId outletId,
    Instant lastSeenAt,
    Instant activatedAt,
    Instant blockedAt,
    UserId blockedBy,
    String blockedReason,
    Instant disabledAt
) {
    private static final BigDecimal MIN_RATE = BigDecimal.ZERO;
    private static final BigDecimal MAX_RATE = new BigDecimal("100.00");

    public static SellerTerminal createPending(
        SellerTerminalId id,
        TenantId tenantId,
        String terminalCode,
        String displayName,
        String firstName,
        String lastName,
        String phoneNumber,
        AddressId addressId,
        OutletId outletId,
        BigDecimal commissionRate
    ) {
        validateCommissionRate(commissionRate);
        return new SellerTerminal(
            id, tenantId, terminalCode, firstName, lastName, displayName, phoneNumber, addressId,
            SellerTerminalStatus.PENDING, commissionRate, outletId,
            null, null, null, null, null, null);
    }

    public SellerTerminal activate(Instant now) {
        if (status == SellerTerminalStatus.DISABLED) {
            throw new SellerTerminalStatusException(id, status, "Disabled terminal cannot be activated");
        }
        if (status == SellerTerminalStatus.ACTIVE) return this;
        return new SellerTerminal(
            id, tenantId, terminalCode, firstName, lastName, displayName, phoneNumber, addressId,
            SellerTerminalStatus.ACTIVE, commissionRate, outletId,
            lastSeenAt, now, blockedAt, blockedBy, blockedReason, disabledAt);
    }

    public SellerTerminal block(UserId by, String reason, Instant now) {
        if (status == SellerTerminalStatus.DISABLED) {
            throw new SellerTerminalStatusException(id, status, "Cannot block a disabled terminal");
        }
        if (status == SellerTerminalStatus.BLOCKED) return this;
        return new SellerTerminal(
            id, tenantId, terminalCode, firstName, lastName, displayName, phoneNumber, addressId,
            SellerTerminalStatus.BLOCKED, commissionRate, outletId,
            lastSeenAt, activatedAt, now, by, reason, disabledAt);
    }

    public SellerTerminal unblock(Instant now) {
        if (status != SellerTerminalStatus.BLOCKED) {
            throw new SellerTerminalStatusException(id, status, "Only BLOCKED terminal can be unblocked");
        }
        return new SellerTerminal(
            id, tenantId, terminalCode, firstName, lastName, displayName, phoneNumber, addressId,
            SellerTerminalStatus.ACTIVE, commissionRate, outletId,
            lastSeenAt, activatedAt != null ? activatedAt : now, null, null, null, disabledAt);
    }

    public SellerTerminal disable(Instant now) {
        if (status == SellerTerminalStatus.DISABLED) return this;
        return new SellerTerminal(
            id, tenantId, terminalCode, firstName, lastName, displayName, phoneNumber, addressId,
            SellerTerminalStatus.DISABLED, commissionRate, outletId,
            lastSeenAt, activatedAt, blockedAt, blockedBy, blockedReason, now);
    }

    public SellerTerminal resetAccessMetadata() {
        return new SellerTerminal(
            id, tenantId, terminalCode, firstName, lastName, displayName, phoneNumber, addressId,
            status, commissionRate, outletId,
            null, activatedAt, blockedAt, blockedBy, blockedReason, disabledAt);
    }

    public SellerTerminal updateProfile(
        String displayName,
        String firstName,
        String lastName,
        String phoneNumber,
        AddressId addressId,
        OutletId outletId
    ) {
        return new SellerTerminal(
            id, tenantId, terminalCode, firstName, lastName, displayName, phoneNumber, addressId,
            status, commissionRate, outletId,
            lastSeenAt, activatedAt, blockedAt, blockedBy, blockedReason, disabledAt);
    }

    public SellerTerminal updateCommissionRate(BigDecimal rate) {
        validateCommissionRate(rate);
        return new SellerTerminal(
            id, tenantId, terminalCode, firstName, lastName, displayName, phoneNumber, addressId,
            status, rate, outletId,
            lastSeenAt, activatedAt, blockedAt, blockedBy, blockedReason, disabledAt);
    }

    public boolean canSell() {
        return status == SellerTerminalStatus.ACTIVE;
    }

    private static void validateCommissionRate(BigDecimal rate) {
        if (rate == null) throw new IllegalArgumentException("commissionRate is required");
        if (rate.compareTo(MIN_RATE) < 0 || rate.compareTo(MAX_RATE) > 0) {
            throw new IllegalArgumentException("commissionRate must be in [0.00, 100.00], got: " + rate);
        }
    }
}
