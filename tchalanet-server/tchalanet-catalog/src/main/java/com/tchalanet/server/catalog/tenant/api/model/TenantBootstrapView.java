package com.tchalanet.server.catalog.tenant.api.model;

import com.tchalanet.server.common.types.id.TenantId;
import java.time.ZoneId;
import java.util.Currency;

/**
 * Bootstrap view for tenant context establishment.
 * Minimal fields for authentication/bootstrap flows.
 * Per DOMAIN_TENANT_CATALOG.md: used to build TenantContextInfo.
 * Replaces TenantBootstrapLookup.TenantBootstrapInfo (now via catalog).
 */
public record TenantBootstrapView(
    TenantId tenantId,
    String code,
    String status,           // e.g. ACTIVE, SUSPENDED, ARCHIVED
    String type,             // e.g. COMMERCIAL, PERSONAL
    ZoneId timezone,         // parsed from DB string with fallback to UTC
    Currency currency,       // parsed from DB code with fallback to USD
    String defaultLanguage,  // e.g. "fr"
    String defaultLocale     // e.g. "fr-HT"
) {}
