package com.tchalanet.server.features.pagemodel.dynamic.providers.platformadmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tchalanet.server.platform.ops.api.OpsServiceResourceItem;
import com.tchalanet.server.platform.ops.api.PlatformHealthProbe;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class PlatformAdminOpsDashboardPayloadAssemblerTest {

  @Test
  @DisplayName("resource summary is exposed in the ops dashboard payload")
  void resourceSummary() {
    ObjectProvider<PlatformHealthProbe> healthProvider = mockProvider(null);
    OpsResourceMetricsProvider metricsProvider =
        () ->
            new PlatformAdminOpsDashboardPayloadAssembler.OpsResourceSummaryPayload(
                "2026-06-25T00:00:00Z",
                1,
                0,
                List.of(
                    new OpsServiceResourceItem(
                        "worker",
                        "Worker",
                        "DOWN",
                        null,
                        null,
                        null,
                        null,
                        null,
                        true,
                        null,
                        "CRITICAL",
                        "Worker is down.",
                        "/app/platform/ops/resources",
                        null,
                        null,
                        null)));
    var assembler =
        new PlatformAdminOpsDashboardPayloadAssembler(
            healthProvider, mockProvider(metricsProvider), mockProvider(null), mockProvider(null));

    var payload = assembler.assemble(null);

    assertThat(payload.resourceSummary().criticalCount()).isEqualTo(1);
    assertThat(payload.resourceSummary().services())
        .singleElement()
        .extracting("serviceKey", "status", "severity")
        .containsExactly("worker", "DOWN", "CRITICAL");
  }

  @Test
  @DisplayName("health probe resource provider maps non-UP services to warnings or critical alerts")
  void healthProbeResourceProvider() {
    PlatformHealthProbe probe = mock(PlatformHealthProbe.class);
    when(probe.snapshot())
        .thenReturn(
            Map.of("global", "UP", "components", Map.of("postgres", "DOWN", "redis", "UNKNOWN")));
    var provider = new HealthProbeOpsResourceMetricsProvider(mockProvider(probe), mockProvider());

    var payload = provider.snapshot();

    assertThat(payload.criticalCount()).isEqualTo(1);
    assertThat(payload.warningCount()).isEqualTo(1);
    assertThat(payload.services()).extracting("serviceKey").contains("api", "postgres", "redis");
  }

  @Test
  @DisplayName("scheduler summary is omitted from platform ops dashboard loading")
  void schedulerSummary() {
    var assembler =
        new PlatformAdminOpsDashboardPayloadAssembler(
            mockProvider(null), mockProvider(null), mockProvider(null), mockProvider(null));

    var payload = assembler.assemble(null);

    assertThat(payload.schedulerSummary().registeredCount()).isZero();
    assertThat(payload.schedulerSummary().disabledGateCount()).isZero();
    assertThat(payload.schedulerSummary().failedCount()).isZero();
    assertThat(payload.schedulerSummary().staleCount()).isZero();
    assertThat(payload.schedulerSummary().neverRunCount()).isZero();
    assertThat(payload.schedulerSummary().historyAvailable()).isFalse();
    assertThat(payload.schedulerSummary().items()).isEmpty();
  }

  @SuppressWarnings("unchecked")
  private static <T> ObjectProvider<T> mockProvider(T value) {
    ObjectProvider<T> provider = mock(ObjectProvider.class);
    when(provider.getIfAvailable()).thenReturn(value);
    when(provider.orderedStream()).thenReturn(value == null ? Stream.empty() : Stream.of(value));
    return provider;
  }

  @SuppressWarnings("unchecked")
  private static <T> ObjectProvider<T> mockProvider() {
    ObjectProvider<T> provider = mock(ObjectProvider.class);
    when(provider.getIfAvailable()).thenReturn(null);
    when(provider.orderedStream()).thenReturn(Stream.empty());
    return provider;
  }
}
