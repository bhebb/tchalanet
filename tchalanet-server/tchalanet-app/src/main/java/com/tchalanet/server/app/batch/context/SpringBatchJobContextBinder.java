package com.tchalanet.server.app.batch.context;

import com.tchalanet.server.common.context.TchActorType;
import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.scope.ApiScope;
import com.tchalanet.server.common.context.tenant.TenantContextInfo;
import com.tchalanet.server.common.context.tenant.TenantContextLookup;
import com.tchalanet.server.common.job.context.JobContextBinder;
import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.TenantId;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static com.tchalanet.server.common.observability.TchTraceIds.MDC_REQUEST_ID;

@Component
@RequiredArgsConstructor
public class SpringBatchJobContextBinder implements JobContextBinder {

    private final TenantContextLookup tenantContextLookup;

    @Override
    public void bindPlatform(String actor) {
        bindPlatform(UUID.randomUUID().toString(), actor);
    }

    @Override
    public void bindTenant(TenantId tenantId, String actor) {
        var info = tenantContextLookup.findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown tenant_id: " + tenantId));

        bindTenant(info, UUID.randomUUID().toString(), actor);
    }

    @Override
    public void clear() {
        MDC.clear();
        TchContext.clear();
    }

    private void bindPlatform(String requestId, String actor) {
        var ctx = new TchRequestContext(
            null, null, null, null, actor, null, Set.of(TchRole.SYSTEM), Set.of(),
            Locale.FRENCH, requestId, "batch", "batch", false, null, "active",
            ApiScope.PLATFORM, null, null, ZoneId.of("UTC"), null, null,
            TchActorType.SYSTEM, null, Set.of(), Set.of(), null
        );

        TchContext.set(ctx);
        MDC.put("tenant_code", "PLATFORM");
        MDC.put("tenant_uuid", "");
        MDC.put("tz", "UTC");
        MDC.put("ccy", "");
        MDC.put(MDC_REQUEST_ID, requestId);
        MDC.put("reqId", requestId);
        MDC.put("actor", actor);
    }

    private void bindTenant(
        TenantContextInfo info,
        String requestId,
        String actor
    ) {
        var zone = info.tenantZoneId() == null ? ZoneId.of("UTC") : info.tenantZoneId();

        var ctx = new TchRequestContext(
            info.tenantCode(), info.tenantId().value(), info.tenantCode(), info.tenantId().value(),
            actor, null, Set.of(TchRole.SYSTEM), Set.of(),
            Locale.FRENCH, requestId, "batch", "batch", false, null, "active",
            ApiScope.TENANT, null, info.tenantId(), zone, info.currency(), null,
            TchActorType.SYSTEM, null, Set.of(), Set.of(), null
        );

        TchContext.set(ctx);
        MDC.put("tenant_code", info.tenantCode());
        MDC.put("tenant_uuid", info.tenantId().value().toString());
        MDC.put("tz", zone.getId());
        MDC.put("ccy", info.currency() == null ? "" : info.currency().getCurrencyCode());
        MDC.put(MDC_REQUEST_ID, requestId);
        MDC.put("reqId", requestId);
        MDC.put("actor", actor);
    }
}
