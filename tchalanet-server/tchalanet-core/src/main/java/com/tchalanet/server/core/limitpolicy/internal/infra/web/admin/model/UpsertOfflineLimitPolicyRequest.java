package com.tchalanet.server.core.limitpolicy.internal.infra.web.admin.model;

import com.tchalanet.server.common.types.money.Money;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Duration;

public record UpsertOfflineLimitPolicyRequest(
    boolean offlineEnabled,
    @Positive int batchSize,
    @NotNull Duration validityDuration,
    @NotNull Duration syncAcceptedExtension,
    @Positive int maxTicketCount,
    @NotNull Money maxTotalAmount
) {}
