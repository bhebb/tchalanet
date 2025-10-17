package com.tchalanet.server.services.subscription;

import com.tchalanet.server.dto.ChangePlanRequest;
import com.tchalanet.server.dto.SubscriptionDTO;

public interface ISubscription {
  SubscriptionDTO currentForTenant(String tenantId);

  SubscriptionDTO changePlan(String tenantId, ChangePlanRequest req);

  SubscriptionDTO cancel(String tenantId, boolean atPeriodEnd);

  SubscriptionDTO resume(String tenantId);
}
