package com.tchalanet.server.core.billing.application.command.model;
import com.tchalanet.server.common.types.id.SubscriptionId;
import com.tchalanet.server.common.types.id.TenantId;

import java.util.UUID;

import com.tchalanet.server.common.bus.Command;

import com.tchalanet.server.core.billing.domain.model.Subscription;

public record ChangePlanCommand(SubscriptionId subscriptionId, TenantId tenantId, UUID planId,
                                boolean proration) implements Command<Subscription> {
}
