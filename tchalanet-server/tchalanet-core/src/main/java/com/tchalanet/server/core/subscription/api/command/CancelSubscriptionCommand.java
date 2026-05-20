package com.tchalanet.server.core.subscription.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import jakarta.validation.constraints.NotNull;

/**
 * Command to cancel a tenant subscription.
 * Maps to SUBSCRIPTION_COMMANDS.md (CancelSubscriptionCommand).
 */
public record CancelSubscriptionCommand(
    @NotNull TenantId tenantId,
    String reason,
    String idempotencyKey
) implements Command<CancelSubscriptionResult> {}
