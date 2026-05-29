package com.tchalanet.server.core.payout.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BlockPayoutClaimCommand(
    @NotNull TenantId tenantId,
    @NotNull PayoutId payoutId,
    @NotNull UserId blockedBy,
    @NotBlank String blockReason
) implements Command<PayoutWorkflowResult> {}
