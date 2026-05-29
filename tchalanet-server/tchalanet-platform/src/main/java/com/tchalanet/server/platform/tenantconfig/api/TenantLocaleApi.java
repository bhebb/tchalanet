package com.tchalanet.server.platform.tenantconfig.api;

import com.tchalanet.server.common.types.id.TenantId;

import java.util.Locale;

/** Resolves default locale settings for a tenant. */
public interface TenantLocaleApi {

    Locale resolveDefaultLocale(TenantId tenantId);

    String resolveDefaultLanguage(TenantId tenantId);
}
