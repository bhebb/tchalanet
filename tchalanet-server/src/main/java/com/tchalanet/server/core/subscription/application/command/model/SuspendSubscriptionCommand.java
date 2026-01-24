package com.tchalanet.server.core.subscription.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import jakarta.validation.constraints.NotNull;

/**
 * Command to suspend an active subscription.
 */
public record SuspendSubscriptionCommand(
    @NotNull TenantId tenantId,
    String idempotencyKey
) implements Command<SuspendSubscriptionResult> {}
