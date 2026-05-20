package com.tchalanet.server.core.subscription.api.command;

import com.tchalanet.server.common.types.id.SubscriptionId;

public record CancelSubscriptionResult(
    SubscriptionId subscriptionId
) {}
