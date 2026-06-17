package com.tchalanet.server.app.batch.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.context.TchActorType;
import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.common.context.scope.ApiScope;
import com.tchalanet.server.common.context.tenant.TenantContextInfo;
import com.tchalanet.server.common.context.tenant.TenantContextLookup;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class SpringBatchJobContextBinderTest {

    private TenantContextLookup tenantContextLookup;
    private SpringBatchJobContextBinder binder;

    @BeforeEach
    void setUp() {
        tenantContextLookup = mock(TenantContextLookup.class);
        binder = new SpringBatchJobContextBinder(tenantContextLookup);
    }

    @AfterEach
    void tearDown() {
        TchContext.clear();
        MDC.clear();
    }

    // §10.3 — batch tenant has effective tenant

    @Test
    void batchTenant_hasEffectiveTenant() {
        var tenantId = TenantId.of(UUID.randomUUID());
        var info = new TenantContextInfo(
            tenantId, "acme", Currency.getInstance("HTG"), ZoneId.of("America/Port-au-Prince"));
        when(tenantContextLookup.findById(tenantId)).thenReturn(Optional.of(info));

        binder.bindTenant(tenantId, "scheduler");

        var ctx = TchContext.get();
        assertThat(ctx).isNotNull();
        assertThat(ctx.actorType()).isEqualTo(TchActorType.SYSTEM);
        assertThat(ctx.apiScope()).isEqualTo(ApiScope.TENANT);
        assertThat(ctx.effectiveTenantIdOrNull()).isEqualTo(tenantId);
        assertThat(ctx.tenantZoneId()).isEqualTo(ZoneId.of("America/Port-au-Prince"));
        assertThat(MDC.get("tenant_code")).isEqualTo("acme");
        assertThat(MDC.get("tz")).isEqualTo("America/Port-au-Prince");
    }

    // §10.3 — batch platform has platform scope

    @Test
    void batchPlatform_hasPlatformScope() {
        binder.bindPlatform("scheduler");

        var ctx = TchContext.get();
        assertThat(ctx).isNotNull();
        assertThat(ctx.actorType()).isEqualTo(TchActorType.SYSTEM);
        assertThat(ctx.apiScope()).isEqualTo(ApiScope.PLATFORM);
        assertThat(ctx.effectiveTenantIdOrNull()).isNull();
        assertThat(MDC.get("tenant_code")).isEqualTo("PLATFORM");
    }

    // §10.3 — batch context is cleared after execution

    @Test
    void batchContext_clearedAfterExecution() {
        binder.bindPlatform("scheduler");
        assertThat(TchContext.hasContext()).isTrue();

        binder.clear();

        assertThat(TchContext.currentOrNull()).isNull();
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    // §10.3 — no context leak between jobs

    @Test
    void noContextLeak_betweenJobs() {
        var tenantId = TenantId.of(UUID.randomUUID());
        var info = new TenantContextInfo(
            tenantId, "tenant-a", Currency.getInstance("HTG"), ZoneId.of("UTC"));
        when(tenantContextLookup.findById(tenantId)).thenReturn(Optional.of(info));

        binder.bindTenant(tenantId, "job-a");
        assertThat(TchContext.get().apiScope()).isEqualTo(ApiScope.TENANT);
        binder.clear();

        binder.bindPlatform("job-b");

        var ctx = TchContext.get();
        assertThat(ctx.apiScope()).isEqualTo(ApiScope.PLATFORM);
        assertThat(ctx.effectiveTenantIdOrNull()).isNull();
        assertThat(MDC.get("tenant_code")).isEqualTo("PLATFORM");
    }
}
