package com.tchalanet.server.core.subscription.api.command;

import com.tchalanet.server.common.types.id.SubscriptionId;

public record ChangePlanResult(
    SubscriptionId subscriptionId,
    String oldPlanCode,
    String newPlanCode
) {}
