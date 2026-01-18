package com.tchalanet.server.core.sales.infra.web.model;

import com.tchalanet.server.common.types.enums.BetType;
import com.tchalanet.server.common.types.enums.TicketResultStatus;
import com.tchalanet.server.common.types.enums.TicketSaleStatus;
import com.tchalanet.server.common.types.enums.TicketSettlementStatus;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record TicketResponse(
    TicketId id,
    TenantId tenantId,
    TerminalId terminalId,
    DrawId drawId,
    String ticketCode,
    String publicCode,

    TicketSaleStatus saleStatus,
    TicketResultStatus resultStatus,
    TicketSettlementStatus settlementStatus,

    BigDecimal totalAmount,

    BigDecimal winningAmount,
    Instant resultedAt,

    Instant createdAt,
    Instant updatedAt,

    List<LineResponse> lines
) {
    public record LineResponse(
        String gameCode,
        BetType betType,
        Integer betOption, // nullable; 1..3 for LOTTO4/LOTTO5
        String selection,
        BigDecimal stake,
        BigDecimal oddsSnapshot,
        BigDecimal potentialPayout
    ) {
    }
}
