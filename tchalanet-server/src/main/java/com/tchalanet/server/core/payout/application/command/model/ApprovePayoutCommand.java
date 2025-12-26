package com.tchalanet.server.core.payout.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.TenantId;

public record ApprovePayoutCommand(TenantId tenantId, PayoutId payoutId) implements Command<Void> {
}
