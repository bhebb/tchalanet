package com.tchalanet.server.tenant.domain.ports;

public interface BillingPort {
  BillingResult changePlan(String tenantId, String planCode, boolean proration);

  BillingResult cancelAtPeriodEnd(String tenantId);

  BillingResult resume(String tenantId);
}
