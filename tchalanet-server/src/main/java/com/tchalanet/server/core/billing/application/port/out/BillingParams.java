package com.tchalanet.server.core.billing.application.port.out;

import com.tchalanet.server.common.types.id.SubscriptionId;
import com.tchalanet.server.common.types.id.TenantId;

public record BillingParams(TenantId tenantId, SubscriptionId subscriptionId) {}
