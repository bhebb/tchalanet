package com.tchalanet.server.platform.tenanttheme.internal.service;

import com.tchalanet.server.catalog.theme.api.ThemeCatalog;
import com.tchalanet.server.catalog.theme.api.ThemePresetView;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.tenanttheme.internal.persistence.TenantThemePersistenceAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Resolves fallback theme preset when requested preset is unavailable.
 *
 * DP3 cascade:
 * 1) Tenant default theme (tenant_theme where is_default=true)
 * 2) Platform default (single preset flagged as default; if none, conventional code default-light)
 * 3) Hardcoded safe preset default-light (last resort)
 */
@Service
@RequiredArgsConstructor
public class TenantThemeFallbackService {

    public static final String HARDCODED_SAFE_PRESET = "default-light";

    private final ThemeCatalog themeCatalog;
    private final TenantThemePersistenceAdapter tenantThemeAdapter;

    /**
     * Resolves a fallback preset code for a tenant when the requested preset is unavailable.
     * Never returns null.
     */
    public String resolveFallback(TenantId tenantId, String requestedPresetCode) {

        // 1) Tenant default theme (if a tenant_theme is marked as defaultTheme=true)
        var tenantDefaultTheme = tenantThemeAdapter.findByTenantId(tenantId);
        if (tenantDefaultTheme.isPresent()
            && tenantDefaultTheme.get().isDefault()
            && isAvailable(tenantDefaultTheme.get().presetCode())) {
            return tenantDefaultTheme.get().presetCode();
        }

        // 2) Platform default:
        //    Prefer a single preset flagged as default (if present).
        //    Since ThemeCatalog does not yet expose findDefault(), we derive it from listActive().
        Optional<String> flaggedDefault = themeCatalog.listActive().stream()
            .filter(ThemePresetView::isDefault)
            .map(ThemePresetView::code)
            .findFirst();

        if (flaggedDefault.isPresent() && isAvailable(flaggedDefault.get())) {
            return flaggedDefault.get();
        }

        // 2b) Conventional code fallback (default-light), if exists and active
        var conventional = themeCatalog.findByCode(HARDCODED_SAFE_PRESET);
        if (conventional.isPresent() && conventional.get().active()) {
            return conventional.get().code();
        }

        // 3) Hardcoded safe preset (last resort)
        return HARDCODED_SAFE_PRESET;
    }

    private boolean isAvailable(String code) {
        return themeCatalog.findByCode(code)
            .filter(ThemePresetView::active)
            .isPresent();
    }
}

