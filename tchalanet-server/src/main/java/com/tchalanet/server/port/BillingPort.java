package com.tchalanet.server.port;

public interface BillingPort {
  BillingResult changePlan(String tenantId, String planCode, boolean proration);

  BillingResult cancelAtPeriodEnd(String tenantId);

  BillingResult resume(String tenantId);
}
