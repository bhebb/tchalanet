package com.tchalanet.server.core.subscription.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Command to renew a subscription (extend ends_at).
 * Maps to SUBSCRIPTION_COMMANDS.md (RenewSubscriptionCommand).
 */
public record RenewSubscriptionCommand(
    @NotNull TenantId tenantId,
    @NotNull Instant newEndsAt,
    String idempotencyKey
) implements Command<RenewSubscriptionResult> {}
