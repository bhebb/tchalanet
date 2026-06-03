package com.tchalanet.server.platform.tenantgame.api.model.view;

/**
 * Catalog projection for tenant admin — combines catalog definition with tenant activation status.
 */
public record TenantGameCatalogItemView(
    String gameCode,
    String name,
    String category,
    boolean catalogActive,
    boolean enabledForTenant,
    boolean canEnable,
    String disabledReason
) {}
