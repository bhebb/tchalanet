package com.tchalanet.server.core.subscription.api.command;

import com.tchalanet.server.common.types.id.SubscriptionId;
import com.tchalanet.server.core.subscription.api.model.SubscriptionStatus;

public record SuspendSubscriptionResult(SubscriptionId subscriptionId, SubscriptionStatus status) {}
