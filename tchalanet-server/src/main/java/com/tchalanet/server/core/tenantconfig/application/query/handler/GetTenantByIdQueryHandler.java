package com.tchalanet.server.core.tenantconfig.application.query.handler;

import com.tchalanet.server.catalog.tenant.api.TenantCatalog;
import com.tchalanet.server.catalog.theme.api.ThemeCatalog;
import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.address.application.AddressCrudService;
import com.tchalanet.server.core.address.application.model.AddressView;
import com.tchalanet.server.core.tenantconfig.application.query.model.GetTenantByIdQuery;
import com.tchalanet.server.core.tenantconfig.application.query.model.TenantConfigView;
import lombok.RequiredArgsConstructor;

/**
 * Query handler: Get Tenant by ID with full details including address and theme code.
 * Per command_query_handlers.md:
 * - Read-only operation
 * - No side effects
 * - Uses TenantCatalog for tenant data
 * - Uses AddressCrudService for address data
 * - Uses ThemeCatalog for theme code
 */
@UseCase
@RequiredArgsConstructor
public class GetTenantByIdQueryHandler implements QueryHandler<GetTenantByIdQuery, TenantConfigView> {

    private final TenantCatalog tenantCatalog;
    private final AddressCrudService addressCrudService;
    private final ThemeCatalog themeCatalog;

    @Override
    public TenantConfigView handle(GetTenantByIdQuery q) {
        // Get tenant registry view
        var registryView = tenantCatalog.findRegistryById(q.tenantId())
            .orElseThrow(() -> new IllegalArgumentException("Tenant registry not found: " + q.tenantId()));

        // Fetch address if present
        AddressView addressView = null;
        if (registryView.addressId().isPresent()) {
            addressView = addressCrudService.get(q.tenantId(), registryView.addressId().get()).orElse(null);
        }

        // Fetch theme code if present
        String themeCode = null;
        if (registryView.activeThemeId().isPresent()) {
            themeCode = themeCatalog.findById(registryView.activeThemeId().get())
                .map(themeView -> themeView.code())
                .orElse(null);
        }

        return new TenantConfigView(
            registryView.tenantId(),
            registryView.code(),
            registryView.name(),
            registryView.type(),
            registryView.timezone(),
            registryView.currency(),
            registryView.status(),
            registryView.activeThemeId().orElse(null),
            themeCode,
            addressView
        );
    }
}
