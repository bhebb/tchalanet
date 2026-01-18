package com.tchalanet.server.core.limitpolicy.domain.model;

import com.tchalanet.server.common.types.enums.BetType;
import com.tchalanet.server.common.types.enums.OperationType;
import com.tchalanet.server.common.types.id.AgentId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

public record LimitContext(
    TenantId tenantId,
    DrawId drawId,
    DrawId drawChannelId, // optional (reuse DrawId for channel id if not present differently)
    AgentId agentId,
    TerminalId terminalId,
    OutletId outletId,
    String zoneId, // optional (now string)
    List<String> rangeIds, // optional
    String gameCode, // optional
    OperationType operationType,
    List<BetLine> lines,
    BigDecimal ticketStakeTotal,
    int linesCount,
    Instant now,
    ZoneId timezone) {
  public record BetLine(
      BetType betType,
      String selectionKey,
      BigDecimal stake,
      Short betOption // nullable; matches TicketLine.betOption
      ) {
    public BetLine(BetType betType, String selectionKey, BigDecimal stake) {
      this(betType, selectionKey, stake, null);
    }
  }
}
