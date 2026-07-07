package com.tchalanet.server.features.platformadmin.tenantonboarding;

import com.tchalanet.server.catalog.drawchannel.api.DrawChannelProvisioningApi;
import com.tchalanet.server.catalog.drawchannel.api.model.ProvisioningTenantGameRef;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.catalog.theme.api.ThemeCatalog;
import com.tchalanet.server.catalog.theme.api.ThemePresetView;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.common.context.TchContextScope;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.limitpolicy.BreachOutcome;
import com.tchalanet.server.core.limitpolicy.api.RuleKey;
import com.tchalanet.server.core.limitpolicy.api.command.UpsertLimitAssignmentCommand;
import com.tchalanet.server.core.limitpolicy.api.model.LimitScopeRef;
import com.tchalanet.server.core.pricing.api.command.EnsureDefaultHaitiLotteryOddsCommand;
import com.tchalanet.server.core.subscription.api.command.ApplyTenantPlanCommand;
import com.tchalanet.server.features.platformadmin.tenantonboarding.model.TenantProvisioningDomainStatuses;
import com.tchalanet.server.features.platformadmin.tenantonboarding.model.TenantProvisioningPreviewView;
import com.tchalanet.server.features.platformadmin.tenantonboarding.model.TenantProvisioningProfile;
import com.tchalanet.server.features.platformadmin.tenantonboarding.model.TenantProvisioningRequest;
import com.tchalanet.server.features.platformadmin.tenantonboarding.model.TenantProvisioningResultView;
import com.tchalanet.server.features.tenantadmin.readiness.TenantReadinessAssembler;
import com.tchalanet.server.features.tenantadmin.readiness.model.TenantReadinessView;
import com.tchalanet.server.platform.identity.api.IdentityApi;
import com.tchalanet.server.platform.tenant.api.TenantConfigApi;
import com.tchalanet.server.platform.tenant.api.model.request.CreateTenantRequest;
import com.tchalanet.server.platform.tenant.api.model.request.GetTenantByCodeRequest;
import com.tchalanet.server.platform.tenantgame.api.TenantGameApi;
import com.tchalanet.server.platform.tenantgame.api.model.request.EnsureTenantGamesRequest;
import com.tchalanet.server.platform.tenanttheme.api.TenantThemeApi;
import com.tchalanet.server.platform.tenanttheme.api.model.ApplyTenantThemeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tenant provisioning orchestrator (dashboard-overview-runtime-v1 §tenant-provisioning).
 * <p>
 * Hard rules:
 * - calls owning domain APIs only (no direct INSERTs into other domains)
 * - never copies transactional data
 * - V1 supports MINIMAL / DEFAULT_HAITI_LOTTERY / DEMO profiles only
 * - result includes a freshly-computed TenantReadinessView
 */
@Service
@RequiredArgsConstructor
public class TenantProvisioningOrchestrator {

    private static final String DEFAULT_PROVISIONING_PLAN_CODE = "DEMO";
    private static final String DEFAULT_THEME_PRESET_CODE = "tchalanet";

    private final TenantConfigApi tenantConfigApi;
    private final TenantReadinessAssembler readinessAssembler;
    private final IdentityApi identityApi;
    private final TenantGameApi tenantGameApi;
    private final DrawChannelProvisioningApi drawChannelProvisioningApi;
    private final ThemeCatalog themeCatalog;
    private final TenantThemeApi tenantThemeApi;
    private final CommandBus commandBus;
    private final JsonUtils jsonUtils;

    public TenantProvisioningPreviewView preview(TenantProvisioningRequest request) {
        List<String> domains = new ArrayList<>(includedDomains(request.profile()));
        domains.add("subscription");
        return new TenantProvisioningPreviewView(
            request.profile(),
            domains,
            warnings(request),
            notCopiedData(),
            expectedReadinessSections());
    }

    public TenantProvisioningResultView provision(TenantProvisioningRequest request) {
        ThemePresetView defaultTheme = resolveDefaultThemePreset();
        tenantConfigApi.createTenant(new CreateTenantRequest(
            request.code(),
            request.name(),
            request.type(),
            request.timezone(),
            request.currency(),
            request.defaultCommissionRate(),
            null,
            defaultTheme.id(),
            Boolean.FALSE));

        var created = tenantConfigApi.getTenantByCode(
            new GetTenantByCodeRequest(request.code()));

        TchContextScope.runStartupTenant(
            created.tenantId().value(),
            "tenant-provisioning-theme",
            () -> tenantThemeApi.applyTenantTheme(
                new ApplyTenantThemeRequest(created.tenantId(), defaultTheme.code())));

        TchContextScope.runStartupTenant(
            created.tenantId().value(),
            "tenant-provisioning-catalog",
            () -> ensureProfileCatalog(created.tenantId(), request.profile()));

        var appliedPlanCode = applyProvisioningPlan(created.tenantId(), resolveProvisioningPlanCode(request));

        var domainStatuses = new TenantProvisioningDomainStatuses(
            "CREATED",
            "SEEDED_VIA_LISTENER",
            defaultTheme.code(),
            "DEFAULT",
            "DEFAULT",
            profileGamesStatus(request.profile()),
            profilePricingStatus(request.profile()),
            profileDrawChannelsStatus(request.profile()),
            "TEMPLATES_AVAILABLE",
            profileLimitStatus(request.profile()),
            "PLAN_APPLIED");

        UUID tenantId = created.tenantId().value();

        // Run in new tenant context for RLS — creates admin + computes readiness.
        var contextResult = TchContextScope.runStartupTenantResult(
            tenantId,
            "tenant-provisioning",
            () -> {
                var initialAdmin = provisionInitialAdmin(
                    TenantId.of(tenantId),
                    created.code(),
                    request.initialAdminEmail());
                var readiness = readinessAssembler.assemble(TchContext.currentOrNull());
                return new TenantProvisioningContextResult(readiness, initialAdmin);
            });
        var initialAdmin = contextResult.initialAdmin();

        return new TenantProvisioningResultView(
            tenantId.toString(),
            created.code(),
            request.profile(),
            request.defaultCommissionRate(),
            domainStatuses,
            nextSteps(request.profile(), request.initialAdminEmail()),
            warnings(request),
            appliedPlanCode,
            contextResult.readiness(),
            initialAdmin.userId(),
            blankToNull(request.initialAdminEmail()),
            initialAdmin.credentialStatus(),
            initialAdmin.temporaryPassword(),
            initialAdmin.mustChangePassword(),
            initialAdmin.mustCompleteProfile());
    }

    // --- profile descriptors --------------------------------------------------

    private static List<String> includedDomains(TenantProvisioningProfile profile) {
        return switch (profile) {
            case MINIMAL -> List.of(
                "tenant_identity", "pagemodels", "theme", "settings", "i18n");
            case DEFAULT_HAITI_LOTTERY -> List.of(
                "tenant_identity", "pagemodels", "theme", "settings", "i18n",
                "games", "pricing", "draw_channels", "promotions_templates", "limits_templates");
            case DEMO -> List.of(
                "tenant_identity", "pagemodels", "theme", "settings", "i18n",
                "games", "pricing", "draw_channels", "promotions_templates",
                "limits_templates", "demo_users", "demo_seller_terminals");
        };
    }

    private static List<String> warnings(TenantProvisioningRequest request) {
        List<String> warnings = new ArrayList<>();
        if (request.initialAdminEmail() == null || request.initialAdminEmail().isBlank()) {
            warnings.add("INITIAL_ADMIN_EMAIL_MISSING");
        }
        return warnings;
    }

    private static List<String> notCopiedData() {
        return List.of(
            "tickets", "sales", "audit", "notifications", "stats");
    }

    private static List<String> expectedReadinessSections() {
        return List.of(
            "identity",
            "address",
            "users",
            "seller_terminals",
            "games_pricing",
            "draws",
            "generated_draws",
            "limits",
            "promotions",
            "settings",
            "subscription",
            "i18n",
            "theme",
            "pagemodels");
    }

    private static List<String> nextSteps(TenantProvisioningProfile profile, String initialAdminEmail) {
        boolean adminCreated = initialAdminEmail != null && !initialAdminEmail.isBlank();
        return switch (profile) {
            case MINIMAL -> adminCreated
                ? List.of("CONFIGURE_GAMES", "CONFIGURE_DRAW_CHANNELS", "CREATE_SELLER_TERMINAL",
                "CONFIGURE_SELLER_RULES", "CONFIGURE_LIMITS", "CONFIGURE_ODDS")
                : List.of("CREATE_INITIAL_ADMIN", "CONFIGURE_GAMES", "CONFIGURE_DRAW_CHANNELS",
                "CREATE_SELLER_TERMINAL", "CONFIGURE_SELLER_RULES", "CONFIGURE_LIMITS", "CONFIGURE_ODDS");
            case DEFAULT_HAITI_LOTTERY -> adminCreated
                ? List.of("CREATE_SELLER_TERMINAL", "CONFIGURE_SELLER_RULES")
                : List.of("CREATE_INITIAL_ADMIN", "CREATE_SELLER_TERMINAL", "CONFIGURE_SELLER_RULES");
            case DEMO -> List.of("VERIFY_DEMO_SETUP");
        };
    }

    private static String profileGamesStatus(TenantProvisioningProfile profile) {
        return profile == TenantProvisioningProfile.MINIMAL ? "NONE" : "DEFAULT_LOTTERY";
    }

    private void ensureProfileCatalog(TenantId tenantId, TenantProvisioningProfile profile) {
        if (profile == TenantProvisioningProfile.MINIMAL) {
            return;
        }
        for (String gameCode : defaultHaitiLotteryGameCodes()) {
            tenantGameApi.ensureTenantGame(EnsureTenantGamesRequest.builder()
                .tenantId(tenantId)
                .gameCode(gameCode)
                .build());
        }
        commandBus.execute(new EnsureDefaultHaitiLotteryOddsCommand(tenantId));
        drawChannelProvisioningApi.ensureDefaultHaitiLotteryChannels(
            tenantId,
            tenantGameApi.listGames(tenantId).stream()
                .map(game -> new ProvisioningTenantGameRef(game.tenantGameId(), game.gameCode()))
                .toList());
        ensureDefaultLimits(tenantId);
    }

    private ThemePresetView resolveDefaultThemePreset() {
        return themeCatalog.findByCode(DEFAULT_THEME_PRESET_CODE)
            .filter(ThemePresetView::active)
            .or(themeCatalog::findDefault)
            .filter(ThemePresetView::active)
            .orElseThrow(() -> new IllegalStateException(
                "No active default theme preset found for tenant provisioning"));
    }

    private static String resolveProvisioningPlanCode(TenantProvisioningRequest request) {
        String requestedPlanCode = request.planCode() == null ? null : request.planCode().trim();
        return requestedPlanCode == null || requestedPlanCode.isBlank()
            ? DEFAULT_PROVISIONING_PLAN_CODE
            : requestedPlanCode;
    }

    private String applyProvisioningPlan(TenantId tenantId, String planCode) {
        TchContextScope.runStartupTenant(
            tenantId.value(),
            "tenant-provisioning-plan",
            () -> commandBus.execute(new ApplyTenantPlanCommand(tenantId, planCode, null, null)));
        return planCode;
    }

    private InitialAdminProvisioningResult provisionInitialAdmin(
        TenantId tenantId,
        String tenantCode,
        String initialAdminEmail) {
        if (initialAdminEmail == null || initialAdminEmail.isBlank()) {
            return InitialAdminProvisioningResult.empty();
        }

        var adminResult = identityApi.createTenantUser(
            tenantId,
            tenantCode,
            initialAdminEmail,
            null,
            null,
            TchRole.TENANT_ADMIN);
        String credentialStatus = adminResult.temporaryCredentialIssued()
            ? "TEMPORARY_PASSWORD_ISSUED"
            : (adminResult.created() ? "TEMPORARY_CREDENTIAL_NOT_RETURNED" : "EXISTING_USER_ATTACHED");
        return new InitialAdminProvisioningResult(
            adminResult.userId().value().toString(),
            credentialStatus,
            adminResult.temporaryPassword(),
            Boolean.TRUE,
            Boolean.TRUE);
    }

    private static List<String> defaultHaitiLotteryGameCodes() {
        return Arrays.stream(GameCode.values())
            .map(GameCode::name)
            .toList();
    }

    private static String profilePricingStatus(TenantProvisioningProfile profile) {
        return profile == TenantProvisioningProfile.MINIMAL ? "NONE" : "DEFAULT";
    }

    private static String profileDrawChannelsStatus(TenantProvisioningProfile profile) {
        return profile == TenantProvisioningProfile.MINIMAL ? "NONE" : "DEFAULT_HAITI";
    }

    private void ensureDefaultLimits(TenantId tenantId) {
        var scope = LimitScopeRef.tenant(tenantId);
        upsertTenantLimit(tenantId, scope, RuleKey.MAX_LINES_PER_TICKET, "maxCount", 200);
        upsertTenantLimit(tenantId, scope, RuleKey.MAX_STAKE_PER_LINE, "valueCents", 10_000_000L);
        upsertTenantLimit(tenantId, scope, RuleKey.MAX_STAKE_PER_TICKET, "valueCents", 100_000_000L);
        upsertTenantLimit(tenantId, scope, RuleKey.MAX_POTENTIAL_PAYOUT_PER_LINE, "valueCents", 1_000_000_000L);
        upsertTenantLimit(tenantId, scope, RuleKey.MAX_POTENTIAL_PAYOUT_PER_TICKET, "valueCents", 5_000_000_000L);
        upsertTenantLimit(tenantId, scope, RuleKey.MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW, "valueCents", 50_000_000L);
        upsertTenantLimit(tenantId, scope, RuleKey.MAX_POTENTIAL_PAYOUT_EXPOSURE_PER_SELECTION_PER_DRAW, "valueCents", 5_000_000_000L);
    }

    private void upsertTenantLimit(
        TenantId tenantId,
        LimitScopeRef scope,
        RuleKey ruleKey,
        String paramKey,
        long paramValue) {
        commandBus.execute(new UpsertLimitAssignmentCommand(
            tenantId,
            ruleKey,
            scope,
            true,
            BreachOutcome.BLOCK,
            jsonUtils.toJsonNode(Map.of(paramKey, paramValue)),
            null,
            null));
    }

    private static String profileLimitStatus(TenantProvisioningProfile profile) {
        return profile == TenantProvisioningProfile.MINIMAL ? "NONE" : "DEFAULT";
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record TenantProvisioningContextResult(
        TenantReadinessView readiness,
        InitialAdminProvisioningResult initialAdmin) {
    }

    private record InitialAdminProvisioningResult(
        String userId,
        String credentialStatus,
        String temporaryPassword,
        Boolean mustChangePassword,
        Boolean mustCompleteProfile) {

        private static InitialAdminProvisioningResult empty() {
            return new InitialAdminProvisioningResult(null, null, null, null, null);
        }
    }
}
