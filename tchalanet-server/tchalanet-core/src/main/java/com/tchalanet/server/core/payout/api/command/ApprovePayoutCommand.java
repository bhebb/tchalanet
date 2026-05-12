package com.tchalanet.server.core.payout.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import jakarta.validation.constraints.NotNull;

public record ApprovePayoutCommand(
    @NotNull TenantId tenantId, @NotNull PayoutId payoutId, @NotNull UserId approvedBy) implements Command<PayoutWorkflowResult> {}
