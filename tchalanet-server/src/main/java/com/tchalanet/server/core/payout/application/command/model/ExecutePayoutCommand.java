package com.tchalanet.server.core.payout.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.core.payout.domain.model.Payout;
import java.util.UUID;

public record ExecutePayoutCommand(UUID tenantId, UUID payoutId, UUID executedBy) implements Command<Payout> {
}

