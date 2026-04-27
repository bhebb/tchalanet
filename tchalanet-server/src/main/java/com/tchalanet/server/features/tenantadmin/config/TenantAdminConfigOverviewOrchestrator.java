package com.tchalanet.server.features.tenantadmin.config;

import com.tchalanet.server.catalog.tenant.api.TenantCatalog;
import com.tchalanet.server.catalog.settings.api.SettingsCatalog;
import com.tchalanet.server.core.autonomy.application.query.model.AutonomyOverviewView;
import com.tchalanet.server.features.tenantadmin.config.model.AdminConfigOverviewView;
import com.tchalanet.server.features.tenantadmin.config.model.ThemeSummaryView;
import com.tchalanet.server.features.tenantadmin.policies.TenantAdminPoliciesOrchestrator;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.enums.AutonomyTargetType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TenantAdminConfigOverviewOrchestrator {

  private final TenantCatalog tenantCatalog;
  private final SettingsCatalog settingsCatalog;
  private final TenantAdminPoliciesOrchestrator policiesOrchestrator;

  public AdminConfigOverviewView getOverview(TchRequestContext ctx) {
    var tenantId = ctx.tenantIdSafe();

    var registry = tenantCatalog.findRegistryById(tenantId).orElseThrow();

    var resolved = settingsCatalog.resolve(new com.tchalanet.server.catalog.settings.api.model.ResolveSettingsCriteria(tenantId, null, null, List.of()));
    int settingsCount = resolved.size();

    // [FIX] Removed ctx argument as per method signature
    var autonomy = policiesOrchestrator.getAutonomyOverview(AutonomyTargetType.TENANT, tenantId.value());

    var theme = new ThemeSummaryView(null, null);

    return new AdminConfigOverviewView(
        new com.tchalanet.server.features.tenantadmin.config.model.TenantIdentityView(
            registry.tenantId().value().toString(), registry.code(), registry.name(), registry.timezone() == null ? null : registry.timezone().toString(), registry.currency() == null ? null : registry.currency().getCurrencyCode(), registry.status().name(), registry.type().name()
        ),
        theme,
        new com.tchalanet.server.features.tenantadmin.config.model.SettingsSummaryView(settingsCount, List.of()),
        new com.tchalanet.server.features.tenantadmin.config.model.I18nSummaryView(List.of(), java.util.Map.of())
    );
  }
}
