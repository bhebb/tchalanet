package com.tchalanet.server.features.bootstrap.privateruntime.app;

import com.tchalanet.server.catalog.i18n.api.I18nOverridesCatalog;
import com.tchalanet.server.catalog.i18n.api.model.I18nBundleView;
import com.tchalanet.server.catalog.i18n.api.model.I18nSurface;
import com.tchalanet.server.catalog.settings.api.SettingsCatalog;
import com.tchalanet.server.catalog.settings.api.model.ResolveSettingsCriteria;
import com.tchalanet.server.catalog.settings.api.model.SettingValueType;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchActorType;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.sellerterminal.api.query.GetSellerTerminalQuery;

import com.tchalanet.server.features.bootstrap.privateruntime.model.AuthenticatedUserView;
import com.tchalanet.server.features.bootstrap.privateruntime.model.EntitlementsView;
import com.tchalanet.server.features.bootstrap.privateruntime.model.PageModelRef;
import com.tchalanet.server.features.bootstrap.privateruntime.model.PrivateBootstrapSpace;
import com.tchalanet.server.features.bootstrap.privateruntime.model.PrivateRuntimeStateResponse;
import com.tchalanet.server.features.bootstrap.privateruntime.model.PrivateRuntimeStatus;
import com.tchalanet.server.features.bootstrap.privateruntime.model.RuntimeBootstrapNotice;
import com.tchalanet.server.features.bootstrap.privateruntime.model.RuntimeBootstrapResponse;
import com.tchalanet.server.features.bootstrap.privateruntime.model.RuntimeI18nBundle;
import com.tchalanet.server.features.bootstrap.privateruntime.model.RuntimeNotificationSummary;
import com.tchalanet.server.features.bootstrap.privateruntime.model.RuntimeReadinessView;
import com.tchalanet.server.features.bootstrap.privateruntime.model.RuntimeSettingsView;
import com.tchalanet.server.features.bootstrap.privateruntime.model.RuntimeThemeView;
import com.tchalanet.server.features.bootstrap.privateruntime.model.RuntimeVersionHints;
import com.tchalanet.server.features.bootstrap.privateruntime.model.TenantContextView;
import com.tchalanet.server.platform.accesscontrol.api.AccessControlApi;
import com.tchalanet.server.platform.accesscontrol.api.model.view.AccessSnapshotView;
import com.tchalanet.server.platform.accesscontrol.api.model.view.AccessSnapshotView.TenantAccessScopeView;
import com.tchalanet.server.platform.identity.api.IdentityApi;
import com.tchalanet.server.platform.identity.api.model.request.GetCurrentUserRequest;
import com.tchalanet.server.platform.identity.api.model.view.CurrentUserView;
import com.tchalanet.server.platform.notification.api.NotificationApi;
import com.tchalanet.server.platform.notification.api.model.request.GetNotificationSummaryRequest;
import com.tchalanet.server.platform.tenant.api.TenantPreContextLookupApi;
import com.tchalanet.server.platform.tenanttheme.api.TenantThemeApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuntimeBootstrapService {

    private final IdentityApi identityApi;
    private final AccessControlApi accessControlApi;
    private final TenantPreContextLookupApi tenantLookup;
    private final TenantThemeApi tenantThemeApi;
    private final SettingsCatalog settingsCatalog;
    private final I18nOverridesCatalog i18nCatalog;
    private final NotificationApi notificationApi;
    private final RuntimeReadinessFacade readinessFacade;
    private final PageModelRefResolver pageModelRefResolver;
    private final QueryBus queryBus;

    public RuntimeBootstrapResponse privateBootstrap(TchRequestContext ctx) {
        if (ctx.actorType() == TchActorType.SELLER_TERMINAL) {
            return sellerTerminalBootstrap(ctx);
        }
        var notices = new ArrayList<RuntimeBootstrapNotice>();

        CurrentUserView currentUser = identityApi.getCurrentUser(
            new GetCurrentUserRequest(ctx.currentUserIdRequired()));
        AccessSnapshotView accessSnapshot = accessControlApi.resolveUserAccess(ctx.currentUserIdRequired());
        PrivateBootstrapSpace space = resolveSpace(ctx, accessSnapshot);

        TenantContextView tenantCtx = resolveTenantContext(ctx, space);
        EntitlementsView entitlements = resolveEntitlements(ctx, accessSnapshot, space);
        RuntimeSettingsView settings = resolveSettings(ctx, currentUser, space);
        RuntimeThemeView theme = resolveTheme(ctx, settings.locale(), space, notices);
        RuntimeI18nBundle i18n = resolveI18n(settings.locale(), space, notices);
        RuntimeReadinessView readiness = readinessFacade.readiness(ctx, space);
        RuntimeNotificationSummary notifications = resolveNotifications(ctx, currentUser, notices);
        PageModelRef pageModelRef = pageModelRefResolver.resolve(space);
        String entryRoute = resolveEntryRoute(currentUser, accessSnapshot, pageModelRef);

        return new RuntimeBootstrapResponse(
            space,
            buildUserView(currentUser, accessSnapshot, space),
            tenantCtx,
            settings,
            theme,
            i18n,
            entitlements,
            readiness,
            notifications,
            pageModelRef,
            entryRoute,
            notices.isEmpty() ? null : notices);
    }

    public PrivateRuntimeStateResponse privateState(TchRequestContext ctx) {
        if (ctx.actorType() == TchActorType.SELLER_TERMINAL) {
            return sellerTerminalState(ctx);
        }
        var notices = new ArrayList<RuntimeBootstrapNotice>();

        CurrentUserView currentUser = identityApi.getCurrentUser(
            new GetCurrentUserRequest(ctx.currentUserIdRequired()));
        AccessSnapshotView accessSnapshot = accessControlApi.resolveUserAccess(ctx.currentUserIdRequired());
        PrivateBootstrapSpace space = resolveSpace(ctx, accessSnapshot);

        RuntimeReadinessView readiness = readinessFacade.readiness(ctx, space);
        RuntimeNotificationSummary notifications = resolveNotifications(ctx, currentUser, notices);

        PrivateRuntimeStatus status = switch (readiness.status()) {
            case READY   -> PrivateRuntimeStatus.READY;
            case PARTIAL -> PrivateRuntimeStatus.PARTIAL;
            case BLOCKED -> PrivateRuntimeStatus.BLOCKED;
        };

        RuntimeVersionHints versions = RuntimeVersionHints.of("boot-v1");

        return new PrivateRuntimeStateResponse(
            status, readiness, notifications, null, versions,
            notices.isEmpty() ? null : notices);
    }

    // ── seller terminal ───────────────────────────────────────────────────────

    private RuntimeBootstrapResponse sellerTerminalBootstrap(TchRequestContext ctx) {
        var terminal = queryBus.ask(new GetSellerTerminalQuery(
            ctx.effectiveTenantIdRequired(), ctx.sellerTerminalIdRequired()));
        var space = PrivateBootstrapSpace.CASHIER;
        var notices = new ArrayList<RuntimeBootstrapNotice>();
        var user = new AuthenticatedUserView(
            terminal.id().value().toString(),
            terminal.terminalCode(),
            terminal.displayName(),
            null,
            List.of("ACTOR_SELLER_TERMINAL"),
            space,
            "fr",
            "UTC",
            false,
            false,
            null,
            null);
        var tenantCtx = resolveTenantContext(ctx, space);
        var entitlements = new EntitlementsView(List.of("ACTOR_SELLER_TERMINAL"), List.of());
        var features = resolveFeatureFlags(ctx, space);
        var settings = new RuntimeSettingsView("fr", "UTC",
            ctx.tenantCurrency() != null ? ctx.tenantCurrency().getCurrencyCode() : "HTG",
            features);
        var theme = resolveTheme(ctx, settings.locale(), space, notices);
        var i18n = resolveI18n(settings.locale(), space, notices);
        var readiness = readinessFacade.readiness(ctx, space);
        var pageModelRef = pageModelRefResolver.resolve(space);
        return new RuntimeBootstrapResponse(
            space, user, tenantCtx, settings, theme, i18n, entitlements, readiness,
            RuntimeNotificationSummary.empty(), pageModelRef, pageModelRef.route(),
            notices.isEmpty() ? null : notices);
    }

    private PrivateRuntimeStateResponse sellerTerminalState(TchRequestContext ctx) {
        var readiness = readinessFacade.readiness(ctx, PrivateBootstrapSpace.CASHIER);
        PrivateRuntimeStatus status = switch (readiness.status()) {
            case READY   -> PrivateRuntimeStatus.READY;
            case PARTIAL -> PrivateRuntimeStatus.PARTIAL;
            case BLOCKED -> PrivateRuntimeStatus.BLOCKED;
        };
        return new PrivateRuntimeStateResponse(
            status, readiness, RuntimeNotificationSummary.empty(), null,
            RuntimeVersionHints.of("boot-v1"), null);
    }

    // ── space resolution ──────────────────────────────────────────────────────

    private PrivateBootstrapSpace resolveSpace(TchRequestContext ctx, AccessSnapshotView accessSnapshot) {
        if (accessSnapshot.platform() != null && accessSnapshot.platform().superAdmin()) {
            return PrivateBootstrapSpace.PLATFORM;
        }
        if (hasTenantRole(accessSnapshot, "TENANT_OWNER") || hasTenantRole(accessSnapshot, "TENANT_ADMIN")) {
            return PrivateBootstrapSpace.ADMIN;
        }
        if (hasTenantRole(accessSnapshot, "OPERATOR") || hasTenantRole(accessSnapshot, "CASHIER")) {
            return PrivateBootstrapSpace.CASHIER;
        }
        TchRole role = ctx.currentRole();
        if (role == null) return PrivateBootstrapSpace.ADMIN;
        return switch (role) {
            case SUPER_ADMIN  -> PrivateBootstrapSpace.PLATFORM;
            case TENANT_ADMIN -> PrivateBootstrapSpace.ADMIN;
            case OPERATOR, CASHIER -> PrivateBootstrapSpace.CASHIER;
            default -> throw ProblemRest.forbidden("runtime.bootstrap.unsupported_role");
        };
    }

    private boolean hasTenantRole(AccessSnapshotView accessSnapshot, String roleCode) {
        return accessSnapshot.tenantScopes() != null
            && accessSnapshot.tenantScopes().stream()
                .anyMatch(scope -> scope.roleCodes() != null && scope.roleCodes().contains(roleCode));
    }

    // ── tenant context ────────────────────────────────────────────────────────

    private TenantContextView resolveTenantContext(TchRequestContext ctx, PrivateBootstrapSpace space) {
        if (space == PrivateBootstrapSpace.PLATFORM) return null;

        var tenantId = ctx.effectiveTenantIdOrNull();
        if (tenantId == null) return null;
        return tenantLookup.findById(tenantId)
            .map(t -> new TenantContextView(
                t.tenantId().value().toString(),
                t.code(),
                t.name()))
            .orElse(new TenantContextView(
                tenantId.value().toString(),
                ctx.effectiveTenantCode(),
                null));
    }

    // ── user view ─────────────────────────────────────────────────────────────

    private AuthenticatedUserView buildUserView(
        CurrentUserView user, AccessSnapshotView accessSnapshot, PrivateBootstrapSpace space) {
        List<String> roles = rolesForSpace(accessSnapshot, space);
        return new AuthenticatedUserView(
            user.id() != null ? user.id().value().toString() : null,
            user.username(),
            user.displayName(),
            user.email(),
            roles,
            space,
            user.locale(),
            user.timeZone(),
            user.mustChangePassword(),
            user.mustCompleteProfile(),
            user.firstLoginCompletedAt(),
            user.temporaryCredentialIssuedAt());
    }

    private String resolveEntryRoute(
        CurrentUserView user, AccessSnapshotView accessSnapshot, PageModelRef pageModelRef) {
        if (user.mustChangePassword() || user.mustCompleteProfile()) {
            return "/app/account/activation";
        }
        if (accessSnapshot.platform() != null && accessSnapshot.platform().superAdmin()) {
            return "/app/platform";
        }
        if (accessSnapshot.tenantScopes() != null && accessSnapshot.tenantScopes().size() > 1) {
            return "/app/select-tenant";
        }
        if (accessSnapshot.tenantScopes() != null
            && accessSnapshot.tenantScopes().size() == 1
            && isTenantAdminScope(accessSnapshot.tenantScopes().getFirst())) {
            return "/app/admin";
        }
        if (accessSnapshot.tenantScopes() == null || accessSnapshot.tenantScopes().isEmpty()) {
            return "/app/access-not-configured";
        }
        return pageModelRef.route();
    }

    // ── entitlements ──────────────────────────────────────────────────────────

    private EntitlementsView resolveEntitlements(
        TchRequestContext ctx, AccessSnapshotView accessSnapshot, PrivateBootstrapSpace space) {
        var roles = rolesForSpace(accessSnapshot, space);
        if (space == PrivateBootstrapSpace.PLATFORM) {
            return new EntitlementsView(
                roles,
                accessSnapshot.platform() == null
                    ? List.of()
                    : List.copyOf(accessSnapshot.platform().permissionKeys()));
        }

        var tenantScope = tenantScopeForContext(ctx, accessSnapshot);
        return new EntitlementsView(
            roles,
            tenantScope == null ? List.of() : List.copyOf(tenantScope.permissionKeys()));
    }

    private List<String> rolesForSpace(AccessSnapshotView accessSnapshot, PrivateBootstrapSpace space) {
        if (space == PrivateBootstrapSpace.PLATFORM && accessSnapshot.platform() != null) {
            return List.copyOf(accessSnapshot.platform().roleCodes());
        }
        if (accessSnapshot.tenantScopes() == null) {
            return List.of();
        }
        return accessSnapshot.tenantScopes().stream()
            .flatMap(scope -> scope.roleCodes().stream())
            .distinct()
            .toList();
    }

    private TenantAccessScopeView tenantScopeForContext(
        TchRequestContext ctx, AccessSnapshotView accessSnapshot) {
        if (accessSnapshot.tenantScopes() == null || accessSnapshot.tenantScopes().isEmpty()) {
            return null;
        }
        var tenantId = ctx.effectiveTenantIdOrNull();
        if (tenantId == null) {
            return accessSnapshot.tenantScopes().size() == 1 ? accessSnapshot.tenantScopes().getFirst() : null;
        }
        return accessSnapshot.tenantScopes().stream()
            .filter(scope -> tenantId.equals(scope.tenantId()))
            .findFirst()
            .orElse(null);
    }

    private boolean isTenantAdminScope(TenantAccessScopeView scope) {
        return scope.roleCodes() != null
            && (scope.roleCodes().contains("TENANT_OWNER") || scope.roleCodes().contains("TENANT_ADMIN"));
    }

    // ── settings ──────────────────────────────────────────────────────────────

    private RuntimeSettingsView resolveSettings(
        TchRequestContext ctx, CurrentUserView user, PrivateBootstrapSpace space) {
        String locale = user.locale() != null ? user.locale() : "fr";
        String timezone = user.timeZone() != null ? user.timeZone()
            : (user.tenantTimeZone() != null ? user.tenantTimeZone() : "UTC");
        String currency = user.currency() != null ? user.currency()
            : (user.tenantCurrency() != null ? user.tenantCurrency() : "USD");

        Map<String, Boolean> features = resolveFeatureFlags(ctx, space);
        return new RuntimeSettingsView(locale, timezone, currency, features);
    }

    private Map<String, Boolean> resolveFeatureFlags(TchRequestContext ctx, PrivateBootstrapSpace space) {
        if (space == PrivateBootstrapSpace.PLATFORM || ctx.tenantId() == null) {
            return Map.of();
        }
        try {
            var resolved = settingsCatalog.resolve(
                ResolveSettingsCriteria.forTenant(ctx.tenantId(), List.of()));
            var flags = new HashMap<String, Boolean>();
            for (var s : resolved) {
                if (s.valueType() == SettingValueType.BOOLEAN) {
                    flags.put(s.fullKey(), Boolean.parseBoolean(s.settingValue()));
                }
            }
            return Map.copyOf(flags);
        } catch (Exception e) {
            log.warn("runtime.bootstrap: failed to load feature flags for tenant {}", ctx.tenantId(), e);
            return Map.of();
        }
    }

    // ── theme ─────────────────────────────────────────────────────────────────

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
            case ADMIN    -> Set.of(I18nSurface.TENANT_ADMIN,   I18nSurface.COMMON_PRIVATE_ERROR);
            case CASHIER  -> Set.of(I18nSurface.CASHIER,        I18nSurface.COMMON_PRIVATE_ERROR);
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
