package com.tchalanet.server.catalog.billing.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.SubscriptionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.catalog.billing.domain.model.Subscription;
import java.util.UUID;

public record ChangePlanCommand(
    SubscriptionId subscriptionId, TenantId tenantId, UUID planId, boolean proration)
    implements Command<Subscription> {}
