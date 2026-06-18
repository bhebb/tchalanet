package com.tchalanet.server.core.sellerterminal.api.model;

import com.tchalanet.server.common.types.id.AddressId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

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
    // activity
    Instant lastSeenAt,
    Instant activatedAt,
    // block state
    Instant blockedAt,
    UserId blockedBy,
    String blockedReason,
    Instant disabledAt
) {}
