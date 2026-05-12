package com.tchalanet.server.core.subscription.api.command;

import com.tchalanet.server.common.types.id.SubscriptionId;
import com.tchalanet.server.core.subscription.internal.domain.model.SubscriptionStatus;

public record ResumeSubscriptionResult(
    SubscriptionId subscriptionId,
    SubscriptionStatus status
) {}
