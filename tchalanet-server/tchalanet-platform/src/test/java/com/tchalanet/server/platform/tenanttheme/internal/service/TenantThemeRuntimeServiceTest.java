package com.tchalanet.server.platform.tenanttheme.internal.service;

import com.tchalanet.server.catalog.theme.api.ThemeCatalog;
import com.tchalanet.server.catalog.theme.api.ThemePresetStatsView;
import com.tchalanet.server.catalog.theme.api.ThemePresetView;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.ThemePresetId;
import com.tchalanet.server.platform.tenanttheme.api.model.ThemeRuntimeView;
import com.tchalanet.server.platform.tenanttheme.internal.persistence.TenantThemePersistenceAdapter;
import tools.jackson.databind.json.JsonMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TenantThemeRuntimeServiceTest {

    private static final TenantId TENANT = TenantId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final String PRESET_CODE = "default-light";
    private static final String CONFIG_JSON = """
        {
          "modes": ["light", "dark"],
          "defaultMode": "light",
          "tokens": {
            "light": { "color.primary": "#6750A4", "color.surface": "#FFFFFF" },
            "dark":  { "color.primary": "#D0BCFF", "color.surface": "#141218" }
          },
          "editableTokens": ["color.primary"],
          "allowedFonts": ["roboto"]
        }
        """;

    private ThemeCatalog themeCatalog;
    private TenantThemePersistenceAdapter persistence;
    private TenantThemeFallbackService fallback;
    private TenantThemeRuntimeService service;
    private JsonUtils jsonUtils;

    @BeforeEach
    void setUp() {
        themeCatalog = mock(ThemeCatalog.class);
        persistence = mock(TenantThemePersistenceAdapter.class);
        fallback = mock(TenantThemeFallbackService.class);
        jsonUtils = new JsonUtils(JsonMapper.builder().build());
        service = new TenantThemeRuntimeService(themeCatalog, persistence, fallback);
    }

    private ThemePresetView activePreset(String code) {
        var config = jsonUtils.parse(CONFIG_JSON);
        return new ThemePresetView(ThemePresetId.of(UUID.randomUUID()), code, "tchalanet",
            config, null, true, true, Instant.now(), Instant.now());
    }

    private TenantTheme tenantTheme(String presetCode, String defaultMode) {
        return new TenantTheme(TENANT, presetCode, defaultMode, true, false, 1L, null,
            Instant.now(), Instant.now(), "system");
    }

    @Test
    void returnsTokensForRequestedLightMode() {
        when(persistence.findActiveByTenantId(TENANT)).thenReturn(Optional.of(tenantTheme(PRESET_CODE, "SYSTEM")));
        when(themeCatalog.findByCode(PRESET_CODE)).thenReturn(Optional.of(activePreset(PRESET_CODE)));

        ThemeRuntimeView view = service.getRuntime(TENANT, "light");

        assertThat(view.mode()).isEqualTo("light");
        assertThat(view.tokens()).containsKey("color.primary");
        assertThat(view.tokens().get("color.primary")).isEqualTo("#6750A4");
        assertThat(view.presetCode()).isEqualTo(PRESET_CODE);
    }

    @Test
    void returnsTokensForDarkMode() {
        when(persistence.findActiveByTenantId(TENANT)).thenReturn(Optional.of(tenantTheme(PRESET_CODE, "DARK")));
        when(themeCatalog.findByCode(PRESET_CODE)).thenReturn(Optional.of(activePreset(PRESET_CODE)));

        ThemeRuntimeView view = service.getRuntime(TENANT, null);

        assertThat(view.mode()).isEqualTo("dark");
        assertThat(view.tokens().get("color.primary")).isEqualTo("#D0BCFF");
    }

    @Test
    void systemModeDefaultsToLight() {
        when(persistence.findActiveByTenantId(TENANT)).thenReturn(Optional.of(tenantTheme(PRESET_CODE, "SYSTEM")));
        when(themeCatalog.findByCode(PRESET_CODE)).thenReturn(Optional.of(activePreset(PRESET_CODE)));

        ThemeRuntimeView view = service.getRuntime(TENANT, null);

        assertThat(view.mode()).isEqualTo("light");
    }

    @Test
    void requestedModeOverridesDefault() {
        when(persistence.findActiveByTenantId(TENANT)).thenReturn(Optional.of(tenantTheme(PRESET_CODE, "LIGHT")));
        when(themeCatalog.findByCode(PRESET_CODE)).thenReturn(Optional.of(activePreset(PRESET_CODE)));

        ThemeRuntimeView view = service.getRuntime(TENANT, "dark");

        assertThat(view.mode()).isEqualTo("dark");
    }

    @Test
    void fallbackAppliedWhenNoActiveTheme() {
        when(persistence.findActiveByTenantId(TENANT)).thenReturn(Optional.empty());
        when(fallback.resolveFallback(TENANT, null)).thenReturn(PRESET_CODE);
        when(themeCatalog.findByCode(PRESET_CODE)).thenReturn(Optional.of(activePreset(PRESET_CODE)));

        ThemeRuntimeView view = service.getRuntime(TENANT, null);

        assertThat(view.isDefault()).isTrue();
        assertThat(view.presetCode()).isEqualTo(PRESET_CODE);
    }

    @Test
    void fallbackAppliedWhenPresetInactive() {
        var inactivePreset = new ThemePresetView(ThemePresetId.of(UUID.randomUUID()), "old-preset", null,
            null, null, false, false, Instant.now(), Instant.now());
        when(persistence.findActiveByTenantId(TENANT)).thenReturn(Optional.of(tenantTheme("old-preset", "SYSTEM")));
        when(themeCatalog.findByCode("old-preset")).thenReturn(Optional.of(inactivePreset));
        when(fallback.resolveFallback(TENANT, "old-preset")).thenReturn(PRESET_CODE);
        when(themeCatalog.findByCode(PRESET_CODE)).thenReturn(Optional.of(activePreset(PRESET_CODE)));

        ThemeRuntimeView view = service.getRuntime(TENANT, null);

        assertThat(view.presetCode()).isEqualTo(PRESET_CODE);
    }

    @Test
    void runtimeViewDoesNotExposeInternalIds() {
        when(persistence.findActiveByTenantId(TENANT)).thenReturn(Optional.of(tenantTheme(PRESET_CODE, "SYSTEM")));
        when(themeCatalog.findByCode(PRESET_CODE)).thenReturn(Optional.of(activePreset(PRESET_CODE)));

        ThemeRuntimeView view = service.getRuntime(TENANT, null);

        assertThat(view.presetCode()).isNotNull();
        assertThat(view.tokens()).isNotNull();
    }
}
