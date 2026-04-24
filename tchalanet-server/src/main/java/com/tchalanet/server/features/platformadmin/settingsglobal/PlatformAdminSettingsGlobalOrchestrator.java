package com.tchalanet.server.features.platformadmin.settingsglobal;

import com.tchalanet.server.catalog.settings.api.SettingsCatalog;
import com.tchalanet.server.catalog.settings.api.model.SettingsCatalogStatsView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlatformAdminSettingsGlobalOrchestrator {

  private final SettingsCatalog settingsCatalog;

  public com.tchalanet.server.features.platformadmin.settingsglobal.SettingsGlobalOverviewView overview() {
    SettingsCatalogStatsView stats = settingsCatalog.stats();
    return new com.tchalanet.server.features.platformadmin.settingsglobal.SettingsGlobalOverviewView(
        stats.totalGlobalSettings(), stats.totalTenantSettings(), stats.totalActiveSettings());
  }
}
