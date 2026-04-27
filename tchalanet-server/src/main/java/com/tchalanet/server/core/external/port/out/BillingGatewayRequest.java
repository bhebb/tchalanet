package com.tchalanet.server.core.external.port.out;

import com.tchalanet.server.common.types.id.SubscriptionId;
import com.tchalanet.server.common.types.id.TenantId;

public record BillingGatewayRequest(
    TenantId tenantId, SubscriptionId subscriptionId, String planExternalKey // nullable
    ) {
  public static BillingGatewayRequest of(TenantId tenantId, SubscriptionId subscriptionId) {
    return new BillingGatewayRequest(tenantId, subscriptionId, null);
  }

  public static BillingGatewayRequest of(
      TenantId tenantId, SubscriptionId subscriptionId, String planExternalKey) {
    return new BillingGatewayRequest(tenantId, subscriptionId, planExternalKey);
  }
}
