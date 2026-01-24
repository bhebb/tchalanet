package com.tchalanet.server.core.subscription.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Command to change the plan of a tenant subscription.
 * Maps to SUBSCRIPTION_COMMANDS.md (ChangePlanCommand).
 */
public record ChangePlanCommand(
    @NotNull TenantId tenantId,
    @NotBlank String newPlanCode,
    Instant effectiveAt,
    String idempotencyKey
) implements Command<ChangePlanResult> {}
