package com.tchalanet.server.core.subscription.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Command to apply a billing plan to a tenant.
 * Maps to spec requirement S1 (apply plan to tenant).
 */
public record ApplyTenantPlanCommand(
    @NotNull TenantId tenantId,
    @NotBlank String planCode,
    Instant effectiveAt,
    String idempotencyKey
) implements Command<ApplyTenantPlanResult> {}
