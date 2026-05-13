package com.tchalanet.server.common.batch.context;

import com.tchalanet.server.common.batch.params.BatchParamKeys;
import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.TenantContextInfo;
import com.tchalanet.server.common.security.ApiScope;
import com.tchalanet.server.common.types.enums.TchRole;
import com.tchalanet.server.common.types.id.TenantId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Binds JobParameters to TchRequestContext for batch jobs.
 * <p>
 * Rules:
 * - If tenant_id is present => TENANT scope (tenant-scoped writes allowed by RLS).
 * - If tenant_id is absent  => PLATFORM scope (no tenant implied; cross-tenant SELECT allowed
 * only if is_super_admin + api_scope=platform; writes must remain safe/global).
 * <p>
 * Timezone/currency are NEVER passed as job parameters.
 * For tenant scope, they are loaded from TenantCatalog bootstrap.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BatchTchContextBinder {

    private final BatchTenantBootstrapProvider tenantBootstrapProvider;

    public void bind(JobParameters jp) {
        String tenantIdRaw = defaultStr(jp.getString(BatchParamKeys.TENANT_ID), null);

        String requestId =
            defaultStr(jp.getString(BatchParamKeys.REQUEST_ID), UUID.randomUUID().toString());
        String actor = defaultStr(jp.getString(BatchParamKeys.ACTOR), "batch");

        // Batch jobs run with SYSTEM role
        Set<TchRole> systemRoles = new HashSet<>();
        systemRoles.add(TchRole.SYSTEM);
        Set<String> customRoles = Set.of();
        Locale locale = Locale.FRENCH;

        if (tenantIdRaw == null) {
            // -------------------------
            // PLATFORM SCOPE (no tenant)
            // -------------------------
            var ctx =
                new TchRequestContext(
                    null,                 // originalTenantCode
                    null,                 // originalTenantUuid
                    null,                 // effectiveTenantCode
                    null,                 // effectiveTenantUuid
                    actor,                // actor
                    null,                 // appUserId
                    systemRoles,
                    customRoles,
                    locale,
                    requestId,
                    "batch",
                    "batch",
                    false,
                    null,
                    "active",
                    ApiScope.PLATFORM,    // ✅ platform scope
                    null,                 // idempotencyKey
                    null,                 // runtime tenantId
                    ZoneId.of("UTC"),     // runtime zone (safe default)
                    null,                  // runtime currency (not applicable),
                    null
                );

            TchContext.set(ctx);

            MDC.put("tenant_code", "PLATFORM");
            MDC.put("tenant_uuid", "");
            MDC.put("tz", "UTC");
            MDC.put("ccy", "");
            MDC.put("reqId", requestId);
            MDC.put("actor", actor);

            log.debug("batch.context.bind scope=PLATFORM requestId={} actor={}", requestId, actor);
            return;
        }

        // -------------------------
        // TENANT SCOPE
        // -------------------------
        TenantId tenantId = TenantId.parse(tenantIdRaw);

        var info =
            tenantBootstrapProvider
                .findBootstrapById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown tenant_id: " + tenantId));

        ZoneId effectiveZone = info.timezone() == null ? ZoneId.of("UTC") : info.timezone();
        var effectiveCurrency = info.currency();

        var tenantCtx = new TenantContextInfo(info.tenantId(), effectiveCurrency, effectiveZone);

        var ctx =
            new TchRequestContext(
                info.code(),                // originalTenantCode
                tenantId.value(),           // originalTenantUuid
                info.code(),                // effectiveTenantCode
                tenantId.value(),           // effectiveTenantUuid
                actor,
                null,
                systemRoles,
                customRoles,
                locale,
                requestId,
                "batch",
                "batch",
                false,
                null,
                "active",
                ApiScope.TENANT,            // ✅ tenant scope
                null,
                tenantCtx.tenantId(),
                tenantCtx.tenantZoneId(),
                tenantCtx.currency(),
                null
            );

        TchContext.set(ctx);

        MDC.put("tenant_code", info.code());
        MDC.put("tenant_uuid", tenantId.value().toString());
        MDC.put("tz", tenantCtx.tenantZoneId().getId());
        MDC.put("ccy", tenantCtx.currency() == null ? "" : tenantCtx.currency().getCurrencyCode());
        MDC.put("reqId", requestId);
        MDC.put("actor", actor);

        log.debug(
            "batch.context.bind scope=TENANT tenantCode={} tenantId={} requestId={} actor={}",
            info.code(),
            tenantId,
            requestId,
            actor);
    }

    public void clear() {
        log.debug("batch.context.clear");
        MDC.clear();
        TchContext.clear();
    }

    private static String defaultStr(String v, String def) {
        if (v == null) return def;
        var t = v.trim();
        return t.isEmpty() ? def : t;
    }
}
