package com.tchalanet.server.core.billing.application.port.out;

import java.util.UUID;

public record BillingParams(UUID tenantId, UUID subscriptionId) {
}
