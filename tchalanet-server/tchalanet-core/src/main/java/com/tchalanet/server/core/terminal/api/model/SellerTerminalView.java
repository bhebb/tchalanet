package com.tchalanet.server.core.terminal.api.model;

import com.tchalanet.server.common.types.id.AddressId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.terminal.internal.domain.model.sellerterminal.SellerTerminal;

import java.math.BigDecimal;
import java.time.Instant;

public record SellerTerminalView(
    SellerTerminalId id,
    TenantId tenantId,
    // identity
    String terminalCode,
    String displayName,
    String firstName,
    String lastName,
    String phoneNumber,
    AddressId addressId,
    // control
    SellerTerminalStatus status,
    BigDecimal commissionRate,
    OutletId outletId,
    // activity
    Instant lastSeenAt,
    Instant activatedAt,
    // block state
    Instant blockedAt,
    UserId blockedBy,
    String blockedReason,
    Instant disabledAt
) {
    public static SellerTerminalView from(SellerTerminal t) {
        return new SellerTerminalView(
            t.id(), t.tenantId(),
            t.terminalCode(), t.displayName(), t.firstName(), t.lastName(),
            t.phoneNumber(), t.addressId(),
            t.status(), t.commissionRate(), t.outletId(),
            t.lastSeenAt(), t.activatedAt(),
            t.blockedAt(), t.blockedBy(), t.blockedReason(), t.disabledAt());
    }
}
