package com.tchalanet.server.core.subscription.application.command.model;

import com.tchalanet.server.common.types.id.SubscriptionId;

import java.time.Instant;

public record RenewSubscriptionResult(
    SubscriptionId subscriptionId,
    Instant newEndsAt
) {}
