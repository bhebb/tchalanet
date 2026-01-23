package com.tchalanet.server.catalog.billing.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.catalog.billing.domain.model.Subscription;

public record CancelSubscriptionCommand(TenantId tenantId, boolean atPeriodEnd)
    implements Command<Subscription> {}
