package com.tchalanet.server.core.limitpolicy.domain.model;

import com.tchalanet.server.common.types.enums.BetType;
import com.tchalanet.server.common.types.enums.OperationType;
import com.tchalanet.server.common.types.id.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

public record LimitContext(
    TenantId tenantId,
    DrawId drawId,
    DrawChannelId drawChannelId, // optional (reuse DrawId for channel id if not present differently)
    AgentId agentId,
    TerminalId terminalId,
    OutletId outletId,
    String zoneId, // optional (now string)
    List<String> rangeIds, // optional
    String gameCode, // optional
    OperationType operationType,
    LimitScopeRef scope,
    List<BetLine> lines,
    BigDecimal ticketStakeTotal,
    int linesCount,
    Instant now,
    ZoneId timezone) {
  public record BetLine(
      BetType betType,
      String selectionKey,
      BigDecimal stake,
      Short betOption,
      BigDecimal potentialPayout
      ) {
    // no explicit constructors — use canonical record constructor
  }
}
