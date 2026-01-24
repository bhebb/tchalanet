package com.tchalanet.server.core.subscription.application.command.model;

import com.tchalanet.server.common.types.id.SubscriptionId;
import com.tchalanet.server.core.subscription.domain.model.SubscriptionStatus;

/**
 * Result of ApplyTenantPlanCommand.
 */
public record ApplyTenantPlanResult(
    SubscriptionId subscriptionId,
    SubscriptionStatus status
) {}
