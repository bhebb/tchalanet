package com.tchalanet.server.core.limitpolicy.api.command.offline;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.limitpolicy.api.model.offline.OfflineLimitPolicy;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Duration;

/**
 * Admin command: upserts the per-tenant offline policy override. When absent, the global
 * defaults from {@code tch.limitpolicy.offline.*} apply.
 */
public record UpsertOfflineLimitPolicyCommand(
    @NotNull TenantId tenantId,
    boolean offlineEnabled,
    @Positive int batchSize,
    @NotNull Duration validityDuration,
    @NotNull Duration syncAcceptedExtension,
    @Positive int maxTicketCount,
    @NotNull Money maxTotalAmount
) implements Command<OfflineLimitPolicy> {}
