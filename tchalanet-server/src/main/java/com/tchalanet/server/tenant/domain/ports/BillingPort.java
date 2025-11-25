package com.tchalanet.server.tenant.domain.ports;

import java.util.UUID;

public interface BillingPort {
  BillingResult changePlan(UUID tenantId, String planCode, boolean proration);

  BillingResult cancelAtPeriodEnd(UUID tenantId);

  BillingResult resume(UUID tenantId);
}
