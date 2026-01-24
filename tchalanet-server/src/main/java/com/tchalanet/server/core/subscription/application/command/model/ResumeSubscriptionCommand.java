package com.tchalanet.server.core.subscription.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import jakarta.validation.constraints.NotNull;

/**
 * Command to resume a suspended subscription.
 */
public record ResumeSubscriptionCommand(
    @NotNull TenantId tenantId,
    String idempotencyKey
) implements Command<ResumeSubscriptionResult> {}
