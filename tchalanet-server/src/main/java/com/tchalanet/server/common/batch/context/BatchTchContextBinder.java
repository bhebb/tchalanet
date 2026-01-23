package com.tchalanet.server.common.batch.context;

import com.tchalanet.server.common.bootstrap.tenant.TenantBootstrapLookup;
import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.TenantContextInfo;
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
 * Binds JobParameters to TchRequestContext for TENANT-scoped batch jobs.
 *
 * Source of truth for tenantZoneId/tenantCurrency is TenantBootstrapLookup (not job parameters).
 * Job parameters may optionally include tenant_zone_id/tenant_currency for safety checks, but they
 * are not used to override tenant settings.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BatchTchContextBinder {

    private final TenantBootstrapLookup tenantLookup;

    /**
     * Bind JobParameters to thread-local context.
     */
    public void bind(JobParameters jp) {
        var tenantIdRaw = defaultStr(jp.getString(BatchParamKeys.TENANT_ID), null);
        if (tenantIdRaw == null) {
            throw new IllegalArgumentException("tenant_id required (job parameter)");
        }

        var tenantId = TenantId.parse(tenantIdRaw);

        var requestId = defaultStr(jp.getString(BatchParamKeys.REQUEST_ID), UUID.randomUUID().toString());
        var actor = defaultStr(jp.getString(BatchParamKeys.ACTOR), "batch");

        // Load tenant info (source of truth) - includes tenantCode
        var info = tenantLookup.findTenantInfoById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown tenant_id: " + tenantId));

        ZoneId effectiveZone = info.tenantZoneId();
        var effectiveCurrency = info.currency();

        // Optional safety checks (do NOT override)
        var zoneParam = defaultStr(jp.getString(BatchParamKeys.TENANT_ZONE_ID), null);
        if (zoneParam != null) {
            var providedZone = parseZoneId(zoneParam);
            if (!providedZone.equals(effectiveZone)) {
                throw new IllegalArgumentException(
                    "tenant_zone_id mismatch. provided=" + providedZone.getId() + " expected=" + effectiveZone.getId());
            }
        }

        var ccyParam = defaultStr(jp.getString(BatchParamKeys.TENANT_CURRENCY), null);
        if (ccyParam != null) {
            var provided = ccyParam.trim().toUpperCase(Locale.ROOT);
            var expected = effectiveCurrency.getCurrencyCode().toUpperCase(Locale.ROOT);
            if (!provided.equals(expected)) {
                throw new IllegalArgumentException(
                    "tenant_currency mismatch. provided=" + provided + " expected=" + expected);
            }
        }

        var tenantCtx = new TenantContextInfo(info.tenantId(), effectiveCurrency, effectiveZone);

        // Batch jobs run with SYSTEM role
        Set<TchRole> systemRoles = new HashSet<>();
        systemRoles.add(TchRole.SYSTEM);
        Set<String> customRoles = Set.of();

        var locale = Locale.FRENCH;

        // Build TchRequestContext using the tenant code (from bootstrap info)
        var ctx = new TchRequestContext(
            info.tenantCode(),                   // originalTenantCode
            tenantId.value(),                    // originalTenantUuid
            info.tenantCode(),                   // effectiveTenantCode
            tenantId.value(),                    // effectiveTenantUuid
            actor,                               // keycloakUserId / actor id
            null,                                // appUserId (not applicable)
            systemRoles,
            customRoles,
            locale,
            requestId,
            "batch",                            // clientIp (not applicable)
            "batch",                            // userAgent
            false,                               // tenantOverridden
            "active",                           // deletedVisibility
            null,                                // idempotencyKey
            tenantCtx.tenantId(),                // runtime tenantId
            tenantCtx.tenantZoneId(),            // runtime tenantZoneId
            tenantCtx.currency()                 // runtime currency
        );

        TchContext.set(ctx);

        // MDC for logs: include both code and uuid
        MDC.put("tenant_code", info.tenantCode());
        MDC.put("tenant_uuid", tenantId.value().toString());
        MDC.put("tz", tenantCtx.tenantZoneId().getId());
        MDC.put("ccy", tenantCtx.currency().getCurrencyCode());
        MDC.put("reqId", requestId);
        MDC.put("actor", actor);

        log.debug("batch.context.bind tenantCode={} tenantId={} requestId={} actor={}",
            info.tenantCode(), tenantId, requestId, actor);
    }

    /**
     * Clear thread-local context and MDC.
     * Must be called after job completes (via BatchJobExecutionListener).
     */
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

    private static ZoneId parseZoneId(String raw) {
        try {
            return ZoneId.of(raw.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid tenant_zone_id: " + raw, e);
        }
    }
}
