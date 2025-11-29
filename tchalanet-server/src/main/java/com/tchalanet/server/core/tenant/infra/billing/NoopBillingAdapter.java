package com.tchalanet.server.core.tenant.infra.billing;

import com.tchalanet.server.core.tenant.domain.ports.BillingPort;
import com.tchalanet.server.core.tenant.domain.ports.BillingResult;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class NoopBillingAdapter implements BillingPort {
  public BillingResult changePlan(UUID tenantId, String planCode, boolean proration) {
    return new BillingResult(
        true, tenantId, Instant.now(), Instant.now().plus(Duration.ofDays(30)), Map.of(), "DEV");
  }

  public BillingResult cancelAtPeriodEnd(UUID tenantId) {
    return new BillingResult(true, tenantId, null, null, Map.of(), "DEV");
  }

  public BillingResult resume(UUID tenantId) {
    return new BillingResult(
        true, tenantId, Instant.now(), Instant.now().plus(Duration.ofDays(30)), Map.of(), "DEV");
  }
}
