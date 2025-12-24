package com.tchalanet.server.core.limitpolicy.domain.model;

import com.tchalanet.server.core.sales.domain.model.BetType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

public record LimitContext(
    UUID tenantId,
    UUID drawId,
    UUID drawChannelId, // optional
    UUID agentId,
    UUID terminalId,
    UUID outletId,
    UUID zoneId, // optional
    List<UUID> rangeIds, // optional
    String gameCode, // optional
    OperationType operationType,
    List<BetLine> lines,
    BigDecimal ticketStakeTotal,
    int linesCount,
    Instant now,
    ZoneId timezone
) {
  public record BetLine(
      BetType betType,
      String selectionKey,
      BigDecimal stake,
      BigDecimal optionalMultiplier // default 1
  ) {
    public BetLine(BetType betType, String selectionKey, BigDecimal stake) {
      this(betType, selectionKey, stake, BigDecimal.ONE);
    }
  }
}
