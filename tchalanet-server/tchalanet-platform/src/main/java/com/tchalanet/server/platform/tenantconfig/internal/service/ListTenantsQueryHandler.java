package com.tchalanet.server.platform.tenantconfig.internal.service;

import com.tchalanet.server.catalog.tenant.api.TenantCatalog;
import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.paging.TchPage;
import com.tchalanet.server.common.paging.TchPageMapper;
import com.tchalanet.server.core.tenantconfig.application.query.model.ListTenantsQuery;
import com.tchalanet.server.core.tenantconfig.application.query.model.TenantConfigView;
import lombok.RequiredArgsConstructor;

/**
 * Query handler: List all tenants with pagination.
 * Per command_query_handlers.md + pagination.md:
 * - Read-only operation
 * - No side effects
 * - Uses TenantCatalog for paginated tenant listing
 * - Returns TchPage<TenantConfigView> (not Spring Page)
 */
@UseCase
@RequiredArgsConstructor
public class ListTenantsQueryHandler implements QueryHandler<ListTenantsQuery, TchPage<TenantConfigView>> {

    private final TenantCatalog tenantCatalog;

    @Override
    public TchPage<TenantConfigView> handle(ListTenantsQuery q) {
        // Get paginated registry views from catalog using Pageable
        var registryPage = tenantCatalog.listTenants(q.pageable());

        // Map to TchPage<TenantConfigView> using TchPages helper
        // Note: theme code excluded from list view for performance (null)
        // Frontend can fetch theme details separately if needed
        return TchPageMapper.map(registryPage, registryView -> new TenantConfigView(
            registryView.tenantId(),
            registryView.code(),
            registryView.name(),
            registryView.type(),
            registryView.timezone(),
            registryView.currency(),
            registryView.status(),
            registryView.activeThemeId().orElse(null),
            null,  // themeCode excluded from list view (performance)
            null   // address not included in list view for performance
        ));
    }
}
