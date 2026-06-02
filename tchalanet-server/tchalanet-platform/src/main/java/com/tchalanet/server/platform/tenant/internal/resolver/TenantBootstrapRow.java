package com.tchalanet.server.platform.tenant.internal.resolver;

import java.util.UUID;

/**
 * Full raw row from the {@code tenant} table, bypassing RLS.
 * Used by {@link TenantRegistryApi} to build both bootstrap and registry views.
 */
public record TenantBootstrapRow(
    UUID id,
    String code,
    String name,
    String status,
    String type,
    String timezone,
    String currency,
    String defaultLanguage,
    String defaultLocale,
    UUID addressId,
    UUID activeThemeId
) {}
