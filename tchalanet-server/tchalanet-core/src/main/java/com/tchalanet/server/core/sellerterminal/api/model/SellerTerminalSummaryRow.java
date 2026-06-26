package com.tchalanet.server.core.sellerterminal.api.model;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;

import java.math.BigDecimal;
import java.time.Instant;

public record SellerTerminalSummaryRow(
    SellerTerminalId id,
    TenantId tenantId,
    String terminalCode,
    String displayName,
    String email,
    String phoneNumber,
    SellerTerminalStatus status,
    BigDecimal commissionRate,
    Instant lastSeenAt,
    Instant activatedAt,
    // Sales stats — populated after S8; null until sales integration
    Long todayTicketCount,
    BigDecimal todaySalesAmount,
    BigDecimal todayCommissionAmount,
    Instant lastSaleAt
) {}
