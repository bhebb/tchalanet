package com.tchalanet.server.core.terminal.api.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.terminal.internal.domain.model.sellerterminal.SellerTerminal;

import java.math.BigDecimal;
import java.time.Instant;

public record SellerTerminalSummaryRow(
    SellerTerminalId id,
    TenantId tenantId,
    String terminalCode,
    String displayName,
    String phoneNumber,
    SellerTerminalStatus status,
    BigDecimal commissionRate,
    OutletId outletId,
    Instant lastSeenAt,
    Instant activatedAt,
    // Sales stats — populated after S8; null until sales integration
    Long todayTicketCount,
    BigDecimal todaySalesAmount,
    BigDecimal todayCommissionAmount,
    Instant lastSaleAt
) {
    public static SellerTerminalSummaryRow from(SellerTerminal t) {
        return new SellerTerminalSummaryRow(
            t.id(), t.tenantId(),
            t.terminalCode(), t.displayName(), t.phoneNumber(),
            t.status(), t.commissionRate(), t.outletId(),
            t.lastSeenAt(), t.activatedAt(),
            null, null, null, null);
    }
}
