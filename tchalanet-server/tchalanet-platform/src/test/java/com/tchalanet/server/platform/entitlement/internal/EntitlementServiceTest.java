package com.tchalanet.server.platform.entitlement.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.platform.entitlement.api.model.TenantCapabilitySnapshot;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EntitlementServiceTest {

  private static final TenantId TENANT_ID =
      TenantId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
  private static final String LIMIT_KEY = "limits.terminals.max";

  @Test
  void limitValueIsEmptyWhenPlanDoesNotDefineLimit() {
    var service = new EntitlementService(getter(Map.of()));

    assertThat(service.limitValue(TENANT_ID, LIMIT_KEY)).isEmpty();
  }

  @Test
  void requireLimitAtMostFailsConfigurationWhenLimitIsMissing() {
    var service = new EntitlementService(getter(Map.of()));

    assertThatThrownBy(() -> service.requireLimitAtMost(TENANT_ID, LIMIT_KEY, 1))
        .isInstanceOf(ProblemRestException.class)
        .hasMessage("Missing entitlement limit: " + LIMIT_KEY);
  }

  @Test
  void requireLimitAtMostAllowsUsageEqualToLimit() {
    var service = new EntitlementService(getter(Map.of(LIMIT_KEY, 2)));

    service.requireLimitAtMost(TENANT_ID, LIMIT_KEY, 2);
  }

  @Test
  void requireLimitAtMostRejectsUsageAboveLimit() {
    var service = new EntitlementService(getter(Map.of(LIMIT_KEY, 2)));

    assertThatThrownBy(() -> service.requireLimitAtMost(TENANT_ID, LIMIT_KEY, 3))
        .isInstanceOf(ProblemRestException.class)
        .hasMessage("entitlement.limit_exceeded");
  }

  private static EntitlementCapabilitiesGetter getter(Map<String, Integer> limits) {
    return tenantId -> new TenantCapabilitySnapshot(
        tenantId,
        "TEST",
        true,
        Map.of(),
        limits,
        Instant.parse("2026-05-25T00:00:00Z")
    );
  }
}
