package com.tchalanet.server.core.tenanttheme.application.service;

import com.tchalanet.server.catalog.theme.api.ThemeCatalog;
import com.tchalanet.server.catalog.theme.api.ThemePresetView;
import com.tchalanet.server.common.types.id.TenantId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Resolves fallback theme preset when requested preset is unavailable.
 * Implements cascade: tenant default → platform default → hardcoded safe.
 * Maps to spec requirement DP3.
 */
@Service
@RequiredArgsConstructor
public class TenantThemeFallbackService {

  private static final String HARDCODED_SAFE_PRESET = "default-light";

  private final ThemeCatalog themeCatalog;
  // TODO: inject TenantConfigPort or TenantCatalog when tenant default is implemented

  /**
   * Resolves fallback preset code for a tenant when requested preset is unavailable.
   *
   * @param tenantId tenant identifier
   * @param unavailablePresetCode preset code that was unavailable (may be null)
   * @return fallback preset code (never null)
   */
  public String resolveFallback(TenantId tenantId, String unavailablePresetCode) {
    // 1. Tenant default (if configured)
    // TODO: implement when tenant config/registry is available
    // var tenantDefault = tenantConfig.getDefaultThemePresetCode(tenantId);
    // if (tenantDefault.isPresent() && isAvailable(tenantDefault.get())) {
    //   return tenantDefault.get();
    // }

    // 2. Platform default (preset flagged as default or conventional code)
    var platformDefault = themeCatalog.findByCode(HARDCODED_SAFE_PRESET);
    if (platformDefault.isPresent() && platformDefault.get().active()) {
      return platformDefault.get().code();
    }

    // 3. Hardcoded safe preset (last resort)
    return HARDCODED_SAFE_PRESET;
  }

  /**
   * Checks if a preset is available (exists and active).
   */
  private boolean isAvailable(String code) {
    return themeCatalog
        .findByCode(code)
        .filter(ThemePresetView::active)
        .isPresent();
  }
}
