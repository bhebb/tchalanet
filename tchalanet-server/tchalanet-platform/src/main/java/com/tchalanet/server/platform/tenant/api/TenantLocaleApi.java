package com.tchalanet.server.platform.tenant.api;

import com.tchalanet.server.common.types.id.TenantId;

import java.util.List;
import java.util.Locale;

/** Resolves locale settings for a tenant. */
public interface TenantLocaleApi {

    Locale resolveDefaultLocale(TenantId tenantId);

    String resolveDefaultLanguage(TenantId tenantId);

    List<String> resolveSupportedLanguages(TenantId tenantId);
}
