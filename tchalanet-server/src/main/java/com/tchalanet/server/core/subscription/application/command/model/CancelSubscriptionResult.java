package com.tchalanet.server.core.subscription.application.command.model;

import com.tchalanet.server.common.types.id.SubscriptionId;

public record CancelSubscriptionResult(
    SubscriptionId subscriptionId
) {}
