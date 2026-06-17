package com.tchalanet.server.features.bootstrap.publicruntime;

import com.tchalanet.server.catalog.i18n.api.I18nOverridesCatalog;
import com.tchalanet.server.catalog.i18n.api.model.I18nBundleView;
import com.tchalanet.server.catalog.i18n.api.model.PublicI18nSurfacePolicy;
import com.tchalanet.server.catalog.settings.api.SettingsAdminCatalog;
import com.tchalanet.server.catalog.settings.api.model.SettingExposure;
import com.tchalanet.server.catalog.settings.api.model.SettingValueType;
import com.tchalanet.server.catalog.settings.api.model.SettingView;
import com.tchalanet.server.features.bootstrap.publicruntime.model.PageModelRef;
import com.tchalanet.server.features.bootstrap.publicruntime.model.PublicBootstrapResponse;
import com.tchalanet.server.features.bootstrap.publicruntime.model.PublicI18nBundle;
import com.tchalanet.server.features.bootstrap.publicruntime.model.PublicSettingsView;
import com.tchalanet.server.features.bootstrap.publicruntime.model.PublicThemeView;
import com.tchalanet.server.platform.tenanttheme.api.TenantThemeApi;
import com.tchalanet.server.platform.tenanttheme.api.model.ThemeRuntimeView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicRuntimeBootstrapService {

    private final SettingsAdminCatalog settingsAdminCatalog;
    private final I18nOverridesCatalog i18nCatalog;
    private final TenantThemeApi tenantThemeApi;

    public PublicBootstrapResponse publicBootstrap(String locale) {
        String lang = (locale != null && !locale.isBlank()) ? locale : "fr";

        PublicSettingsView settings = resolvePublicSettings(lang);
        PublicThemeView theme = resolvePublicTheme();
        PublicI18nBundle i18n = resolvePublicI18n(lang);
        PageModelRef pageModelRef = new PageModelRef("/", "/public/page-model?route=/");

        return new PublicBootstrapResponse(settings, theme, i18n, pageModelRef);
    }

    private PublicSettingsView resolvePublicSettings(String locale) {
        var features = new HashMap<String, Boolean>();
        try {
            for (SettingView s : settingsAdminCatalog.listActiveByExposure(
                SettingExposure.PUBLIC_RUNTIME, null)) {
                if (s.valueType() == SettingValueType.BOOLEAN) {
                    features.put(s.namespace() + "." + s.settingKey(),
                        Boolean.parseBoolean(s.settingValue()));
                }
            }
        } catch (Exception e) {
            log.warn("runtime.public-bootstrap: failed to load public settings", e);
        }
        return new PublicSettingsView(
            locale, "America/Port-au-Prince", List.of("fr", "en", "ht"), "HTG", Map.copyOf(features));
    }

    // Null tenantId → TenantThemeRuntimeService serves the global default preset (seeded as "tchalanet")
    private PublicThemeView resolvePublicTheme() {
        try {
            ThemeRuntimeView tv = tenantThemeApi.resolveTenantThemeRuntime(null, "light");
            return new PublicThemeView(tv.presetCode(), tv.mode(), tv.tokens());
        } catch (Exception e) {
            log.warn("runtime.public-bootstrap: theme resolution failed, using fallback", e);
            return PublicThemeView.fallback();
        }
    }

    private PublicI18nBundle resolvePublicI18n(String locale) {
        try {
            I18nBundleView bundle =
                i18nCatalog.loadBundle(locale, PublicI18nSurfacePolicy.publicSurfaces());
            var merged = new HashMap<String, String>();
            if (bundle.surfaces() != null) {
                bundle.surfaces().values().forEach(merged::putAll);
            }
            return new PublicI18nBundle(bundle.locale(), Map.copyOf(merged), Instant.now().toString());
        } catch (Exception e) {
            log.warn("runtime.public-bootstrap: i18n bundle load failed for locale={}", locale, e);
            return new PublicI18nBundle(locale, Map.of(), Instant.now().toString());
        }
    }
}
