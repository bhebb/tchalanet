package com.tchalanet.server.core.pricing.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;

public record EnsureDefaultHaitiLotteryOddsCommand(
    TenantId tenantId
) implements Command<Void> {}
