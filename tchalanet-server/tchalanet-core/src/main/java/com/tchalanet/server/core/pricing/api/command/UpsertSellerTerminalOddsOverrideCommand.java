package com.tchalanet.server.core.pricing.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

import java.math.BigDecimal;
import java.time.Instant;

public record UpsertSellerTerminalOddsOverrideCommand(
    TenantId tenantId,
    SellerTerminalId sellerTerminalId,
    String gameCode,
    String betType,
    Short betOption,
    BigDecimal odds,
    Instant effectiveFrom,
    Instant effectiveTo,
    String reason,
    UserId actorId
) implements Command<UpsertSellerTerminalOddsOverrideResult> {}
