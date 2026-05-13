package com.tchalanet.server.features.tenantadmin.config;

import com.tchalanet.server.common.context.TchRequestContext;

import com.tchalanet.server.catalog.settings.api.SettingsCatalog;
import com.tchalanet.server.catalog.tenant.api.TenantCatalog;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.enums.AutonomyTargetType;
import com.tchalanet.server.core.autonomy.api.query.GetAutonomyOverviewQuery;
import com.tchalanet.server.features.tenantadmin.config.model.AdminConfigOverviewView;
import com.tchalanet.server.features.tenantadmin.config.model.I18nSummaryView;
import com.tchalanet.server.features.tenantadmin.config.model.SettingsSummaryView;
import com.tchalanet.server.features.tenantadmin.config.model.TenantIdentityView;
import com.tchalanet.server.features.tenantadmin.config.model.ThemeSummaryView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TenantAdminConfigOverviewOrchestrator {

    private final TenantCatalog tenantCatalog;
    private final SettingsCatalog settingsCatalog;
    private final QueryBus queryBus;

    public AdminConfigOverviewView getOverview(TchRequestContext ctx) {
        var tenantId = ctx.tenantIdSafe();

        var registry = tenantCatalog.findRegistryById(tenantId).orElseThrow();

        var resolved = settingsCatalog.resolve(new com.tchalanet.server.catalog.settings.api.model.ResolveSettingsCriteria(tenantId, null, null, List.of()));
        int settingsCount = resolved.size();

        queryBus.ask(new GetAutonomyOverviewQuery(AutonomyTargetType.TENANT, tenantId.value()));

        var theme = new ThemeSummaryView(null, null);

        return new AdminConfigOverviewView(
            new TenantIdentityView(
                registry.tenantId().value().toString(), registry.code(), registry.name(),
                registry.timezone() == null ? null : registry.timezone().toString(),
                registry.currency() == null ? null : registry.currency().getCurrencyCode(),
                registry.status().name(), registry.type().name()),
            theme,
            new SettingsSummaryView(settingsCount, List.of()),
            new I18nSummaryView(List.of(), Map.of()));
    }
}
