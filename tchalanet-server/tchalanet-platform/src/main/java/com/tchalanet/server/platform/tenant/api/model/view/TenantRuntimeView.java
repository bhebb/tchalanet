package com.tchalanet.server.platform.tenant.api.model.view;

import java.time.ZoneId;
import java.util.Currency;
import java.util.List;

/**
 * Safe public/private runtime view of a tenant.
 * Returned by /public/tenant/runtime and /tenant/runtime.
 * Must not expose: raw config JSON, deleted_at, address internals,
 * internal IDs, audit metadata, or admin status reasons.
 */
public record TenantRuntimeView(
    String tenantCode,
    String displayName,
    String statusPublic,
    ZoneId timezone,
    Currency currency,
    String defaultLanguage,
    String defaultLocale,
    List<String> supportedLocales
) {}
