package com.tchalanet.server.core.external.port.out;

import java.util.UUID;

public record BillingGatewayRequest(
    UUID tenantId,
    UUID subscriptionId,
    String planExternalKey // nullable
) {
    public static BillingGatewayRequest of(UUID tenantId, UUID subscriptionId) {
        return new BillingGatewayRequest(tenantId, subscriptionId, null);
    }

    public static BillingGatewayRequest of(UUID tenantId, UUID subscriptionId, String planExternalKey) {
        return new BillingGatewayRequest(tenantId, subscriptionId, planExternalKey);
    }
}
