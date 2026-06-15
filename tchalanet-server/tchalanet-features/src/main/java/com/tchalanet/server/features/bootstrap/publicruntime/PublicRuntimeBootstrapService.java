package com.tchalanet.server.features.bootstrap.publicruntime;

import com.tchalanet.server.catalog.i18n.api.I18nOverridesCatalog;
import com.tchalanet.server.catalog.i18n.api.model.I18nBundleView;
import com.tchalanet.server.catalog.i18n.api.model.I18nSurface;
import com.tchalanet.server.catalog.i18n.api.model.PublicI18nSurfacePolicy;
import com.tchalanet.server.catalog.settings.api.SettingsAdminCatalog;
import com.tchalanet.server.catalog.settings.api.SettingsCatalog;
import com.tchalanet.server.catalog.settings.api.model.SettingExposure;
import com.tchalanet.server.catalog.settings.api.model.SettingValueType;
import com.tchalanet.server.catalog.settings.api.model.SettingView;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.features.bootstrap.publicruntime.model.RuntimeThemeView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicRuntimeBootstrapService {

    private final TenantThemeApi tenantThemeApi;
    private final SettingsCatalog settingsCatalog;
    private final SettingsAdminCatalog settingsAdminCatalog;
    private final I18nOverridesCatalog i18nCatalog;
    private final NotificationApi notificationApi;
    private final RuntimeReadinessFacade readinessFacade;
    private final PageModelRefResolver pageModelRefResolver;

    // ── public bootstrap (unauthenticated) ────────────────────────────────────

    public PublicBootstrapResponse publicBootstrap(String locale) {
        String lang = (locale != null && !locale.isBlank()) ? locale : "fr";
        var notices = new ArrayList<RuntimeBootstrapNotice>();

        PublicSettingsView settings = resolvePublicSettings(lang, notices);
        PublicThemeView theme = PublicThemeView.fallback();
        PublicI18nBundle i18n = resolvePublicI18n(lang, notices);
        PublicNavigationModel navigation = defaultPublicNavigation();
        PublicReadinessView readiness = PublicReadinessView.ready();
        PageModelRef pageModelRef = new PageModelRef("/", "/public/page-model?route=/");

        return new PublicBootstrapResponse(
            settings, theme, i18n, navigation, readiness, pageModelRef,
            notices.isEmpty() ? null : notices);
    }

    private PublicSettingsView resolvePublicSettings(
        String locale, List<RuntimeBootstrapNotice> notices) {
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
            notices.add(RuntimeBootstrapNotice.warning("public.settings.unavailable",
                "Public settings could not be loaded; defaults applied."));
        }
        return new PublicSettingsView(
            locale, "America/Port-au-Prince", List.of("fr", "en", "ht"), "HTG", Map.copyOf(features));
    }

    private PublicI18nBundle resolvePublicI18n(
        String locale, List<RuntimeBootstrapNotice> notices) {
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
            notices.add(RuntimeBootstrapNotice.warning("public.i18n.fallback",
                "Public i18n bundle could not be loaded; frontend fallback will be used."));
            return new PublicI18nBundle(locale, Map.of(), Instant.now().toString());
        }
    }

    private PublicNavigationModel defaultPublicNavigation() {
        var items = List.of(
            PublicNavigationItem.of("home", "nav.public.home", "/"),
            PublicNavigationItem.of("results", "nav.public.results", "/results"),
            PublicNavigationItem.of("rules", "nav.public.rules", "/rules"),
            PublicNavigationItem.of("contact", "nav.public.contact", "/contact"));
        return new PublicNavigationModel(items, null);
    }



 ─theme ─────────────────────────────────────────────────────────────────

    private RuntimeThemeView resolveTheme(
        TchRequestContext ctx, String locale, PrivateBootstrapSpace space,
        List<RuntimeBootstrapNotice> notices) {
        if (space == PrivateBootstrapSpace.PLATFORM || ctx.tenantId() == null) {
            return RuntimeThemeView.fallback();
        }
        try {
            String mode = "light";
            var tv = tenantThemeApi.resolveTenantThemeRuntime(ctx.tenantId(), mode);
            return new RuntimeThemeView(
                tv.presetCode(), tv.mode(), tv.tokens(), tv.isDefault(), tv.version());
        } catch (Exception e) {
            log.warn("runtime.bootstrap: theme resolution failed for tenant {}", ctx.tenantId(), e);
            notices.add(RuntimeBootstrapNotice.warning("theme.fallback",
                "Theme could not be loaded; default applied."));
            return RuntimeThemeView.fallback();
        }
    }

    // ── i18n ──────────────────────────────────────────────────────────────────

    private RuntimeI18nBundle resolveI18n(
        String locale, PrivateBootstrapSpace space, List<RuntimeBootstrapNotice> notices) {
        var surfaces = surfacesFor(space);
        try {
            I18nBundleView bundle = i18nCatalog.loadBundle(locale, surfaces);
            var merged = new HashMap<String, String>();
            if (bundle.surfaces() != null) {
                bundle.surfaces().values().forEach(merged::putAll);
            }
            return new RuntimeI18nBundle(bundle.locale(), Map.copyOf(merged));
        } catch (Exception e) {
            log.warn("runtime.bootstrap: i18n bundle load failed for locale={} space={}", locale, space, e);
            notices.add(RuntimeBootstrapNotice.warning("i18n.fallback",
                "i18n bundle could not be loaded; frontend fallback will be used."));
            return RuntimeI18nBundle.empty(locale);
        }
    }

    private Set<I18nSurface> surfacesFor(PrivateBootstrapSpace space) {
        return switch (space) {
            case PLATFORM -> Set.of(I18nSurface.PLATFORM_ADMIN, I18nSurface.COMMON_PRIVATE_ERROR);
            case ADMIN -> Set.of(I18nSurface.TENANT_ADMIN, I18nSurface.COMMON_PRIVATE_ERROR);
            case CASHIER -> Set.of(I18nSurface.CASHIER, I18nSurface.COMMON_PRIVATE_ERROR);
        };
    }

    // ── notifications ─────────────────────────────────────────────────────────

    private RuntimeNotificationSummary resolveNotifications(
        TchRequestContext ctx, CurrentUserView user,
        List<RuntimeBootstrapNotice> notices) {
        if (ctx.userId() == null) return RuntimeNotificationSummary.empty();
        try {
            String roleCode = ctx.currentRole() != null ? ctx.currentRole().name() : null;
            var summary = notificationApi.getNotificationSummary(
                new GetNotificationSummaryRequest(ctx.userId(), roleCode));
            return RuntimeNotificationSummary.from(summary);
        } catch (Exception e) {
            log.warn("runtime.bootstrap: notification summary failed for user {}", ctx.userId(), e);
            notices.add(RuntimeBootstrapNotice.warning("notifications.unavailable",
                "Notification summary could not be loaded."));
            return RuntimeNotificationSummary.empty();
        }
    }
}
