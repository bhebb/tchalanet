package com.tchalanet.server.core.sales.api.query;

import com.tchalanet.server.common.types.enums.BetType;
import com.tchalanet.server.common.types.enums.GameCode;
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

/**
 * DTO / View for ticket details returned by queries (includes ticket lines).
 */
public record TicketDetailsView(
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

    List<LineView> lines
) {

  public record LineView(
      GameCode gameCode,
      BetType betType,
      Short betOption,
      String selection,
      BigDecimal stake,
      BigDecimal oddsSnapshot,
      BigDecimal potentialPayout) {}
}
