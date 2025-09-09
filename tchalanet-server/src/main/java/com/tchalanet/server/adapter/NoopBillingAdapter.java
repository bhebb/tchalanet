package com.tchalanet.server.adapter;

import com.tchalanet.server.port.BillingPort;
import com.tchalanet.server.port.BillingResult;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
class NoopBillingAdapter implements BillingPort {
  public BillingResult changePlan(String tenantId, String planCode, boolean proration) {
    return new BillingResult(
        true,
        "noop-" + tenantId,
        Instant.now(),
        Instant.now().plus(Duration.ofDays(30)),
        Map.of(),
        "DEV");
  }

  public BillingResult cancelAtPeriodEnd(String tenantId) {
    return new BillingResult(true, "noop-" + tenantId, null, null, Map.of(), "DEV");
  }

  public BillingResult resume(String tenantId) {
    return new BillingResult(
        true,
        "noop-" + tenantId,
        Instant.now(),
        Instant.now().plus(Duration.ofDays(30)),
        Map.of(),
        "DEV");
  }
}
