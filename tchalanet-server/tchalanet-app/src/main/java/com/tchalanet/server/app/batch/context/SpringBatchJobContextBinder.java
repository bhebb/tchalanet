package com.tchalanet.server.app.batch.context;

import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.job.context.JobContextBinder;
import com.tchalanet.server.common.job.context.JobContextBindingRequest;
import com.tchalanet.server.common.job.context.JobExecutionScope;
import com.tchalanet.server.common.job.context.JobTenantBootstrapProvider;
import com.tchalanet.server.common.job.params.JobParamKeys;
import com.tchalanet.server.common.security.ApiScope;
import com.tchalanet.server.common.types.enums.TchRole;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringBatchJobContextBinder implements JobContextBinder {

    private final JobTenantBootstrapProvider tenantBootstrapProvider;

    @Override
    public void bind(JobContextBindingRequest request) {
        var params = request.params();

        var requestId = valueOr(params.get(JobParamKeys.REQUEST_ID), UUID.randomUUID().toString());
        var actor = valueOr(params.get(JobParamKeys.ACTOR), "batch");

        if (request.scope() == JobExecutionScope.PLATFORM) {
            bindPlatform(requestId, actor);
            return;
        }

        var tenantId = TenantId.parse(required(params.get(JobParamKeys.TENANT_ID), JobParamKeys.TENANT_ID));
        var info = tenantBootstrapProvider.findBootstrapById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown tenant_id: " + tenantId));

        bindTenant(info, requestId, actor);
    }

    @Override
    public void clear() {
        MDC.clear();
        TchContext.clear();
    }

    private void bindPlatform(String requestId, String actor) {
        var ctx = new TchRequestContext(
            null,
            null,
            null,
            null,
            actor,
            null,
            Set.of(TchRole.SYSTEM),
            Set.of(),
            Locale.FRENCH,
            requestId,
            "batch",
            "batch",
            false,
            null,
            "active",
            ApiScope.PLATFORM,
            null,
            null,
            ZoneId.of("UTC"),
            null,
            null
        );

        TchContext.set(ctx);
        MDC.put("tenant_code", "PLATFORM");
        MDC.put("tenant_uuid", "");
        MDC.put("tz", "UTC");
        MDC.put("ccy", "");
        MDC.put("reqId", requestId);
        MDC.put("actor", actor);
    }

    private void bindTenant(
        com.tchalanet.server.common.job.context.JobTenantBootstrap info,
        String requestId,
        String actor
    ) {
        var zone = info.timezone() == null ? ZoneId.of("UTC") : info.timezone();

        var ctx = new TchRequestContext(
            info.code(),
            info.tenantId().value(),
            info.code(),
            info.tenantId().value(),
            actor,
            null,
            Set.of(TchRole.SYSTEM),
            Set.of(),
            Locale.FRENCH,
            requestId,
            "batch",
            "batch",
            false,
            null,
            "active",
            ApiScope.TENANT,
            null,
            info.tenantId(),
            zone,
            info.currency(),
            null
        );

        TchContext.set(ctx);
        MDC.put("tenant_code", info.code());
        MDC.put("tenant_uuid", info.tenantId().value().toString());
        MDC.put("tz", zone.getId());
        MDC.put("ccy", info.currency() == null ? "" : info.currency().getCurrencyCode());
        MDC.put("reqId", requestId);
        MDC.put("actor", actor);
    }

    private static String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String required(String value, String key) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Required parameter missing: " + key);
        }
        return value.trim();
    }
}
