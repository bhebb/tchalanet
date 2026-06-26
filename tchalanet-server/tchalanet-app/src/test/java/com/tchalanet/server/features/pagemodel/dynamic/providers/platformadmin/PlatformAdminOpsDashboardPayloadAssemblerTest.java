package com.tchalanet.server.features.pagemodel.dynamic.providers.platformadmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.job.gate.BatchGate;
import com.tchalanet.server.common.job.key.JobKey;
import com.tchalanet.server.common.job.registry.RegisteredJob;
import com.tchalanet.server.common.job.registry.TchJobRegistry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class PlatformAdminOpsDashboardPayloadAssemblerTest {

    @Test
    @DisplayName("resource summary is exposed in the ops dashboard payload")
    void resourceSummary() {
        ObjectProvider<PlatformHealthProbe> healthProvider = mockProvider(null);
        OpsResourceMetricsProvider metricsProvider = () ->
            new PlatformAdminOpsDashboardPayloadAssembler.OpsResourceSummaryPayload(
                "2026-06-25T00:00:00Z",
                1,
                0,
                List.of(new PlatformAdminOpsDashboardPayloadAssembler.OpsServiceResourceItem(
                    "worker", "Worker", "DOWN", null, null, null, null, null, true, null,
                    "CRITICAL", "Worker is down.", "/app/platform/ops/resources", null, null, null)));
        var assembler = new PlatformAdminOpsDashboardPayloadAssembler(
            healthProvider,
            mockProvider(metricsProvider),
            mockProvider(null),
            mockProvider(null),
            mockProvider(null),
            mockProvider(null),
            mockProvider(null));

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
        when(probe.snapshot()).thenReturn(Map.of(
            "global", "UP",
            "components", Map.of("postgres", "DOWN", "redis", "UNKNOWN")));
        var provider = new HealthProbeOpsResourceMetricsProvider(mockProvider(probe), mockProvider());

        var payload = provider.snapshot();

        assertThat(payload.criticalCount()).isEqualTo(1);
        assertThat(payload.warningCount()).isEqualTo(1);
        assertThat(payload.services())
            .extracting("serviceKey")
            .contains("api", "postgres", "redis");
    }

    @Test
    @DisplayName("scheduler summary includes disabled gates and failed job history")
    void schedulerSummary() {
        TchJobRegistry registry = mock(TchJobRegistry.class);
        BatchGate gate = mock(BatchGate.class);
        var jobKey = JobKey.of("results:external:fetch");
        var job = new RegisteredJob(
            jobKey,
            "Fetch results",
            RegisteredJob.JobScope.GLOBAL,
            Set.of(),
            Set.of());
        var disabledJobKey = JobKey.of("draw:lifecycle:generate");
        var disabledJob = new RegisteredJob(
            disabledJobKey,
            "Generate draws",
            RegisteredJob.JobScope.TENANT,
            Set.of(),
            Set.of());
        when(registry.list()).thenReturn(List.of(job, disabledJob));
        when(registry.find(jobKey)).thenReturn(Optional.of(job));
        when(gate.enabled(jobKey, null)).thenReturn(true);
        when(gate.enabled(disabledJobKey, null)).thenReturn(false);
        OpsSchedulerHistoryProvider historyProvider = () -> new OpsSchedulerHistoryProvider.Snapshot(
            2,
            1,
            1,
            true,
            List.of(new OpsSchedulerHistoryProvider.Item(
                jobKey.value(),
                "Fetch results",
                "GLOBAL",
                "FAILED",
                "CRITICAL",
                "/app/platform/ops/batch",
                "NY_EVE")));
        var assembler = new PlatformAdminOpsDashboardPayloadAssembler(
            mockProvider(null),
            mockProvider(null),
            mockProvider(registry),
            mockProvider(gate),
            mockProvider(historyProvider),
            mockProvider(null),
            mockProvider(null));

        var payload = assembler.assemble(null);

        assertThat(payload.schedulerSummary().registeredCount()).isEqualTo(2);
        assertThat(payload.schedulerSummary().disabledGateCount()).isEqualTo(1);
        assertThat(payload.schedulerSummary().failedCount()).isEqualTo(2);
        assertThat(payload.schedulerSummary().staleCount()).isEqualTo(1);
        assertThat(payload.schedulerSummary().neverRunCount()).isEqualTo(1);
        assertThat(payload.schedulerSummary().historyAvailable()).isTrue();
        assertThat(payload.schedulerSummary().items())
            .extracting("severity")
            .contains("WARNING", "CRITICAL");
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
