package com.tchalanet.server.features.platformadmin.tenantonboarding;

import com.tchalanet.server.catalog.drawchannel.api.DrawChannelProvisioningApi;
import com.tchalanet.server.catalog.drawchannel.api.model.ProvisioningTenantGameRef;
import com.tchalanet.server.catalog.pricing.api.PricingProvisioningApi;
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
import com.tchalanet.server.platform.tenant.internal.adapter.TenantPersistenceAdapter;
import com.tchalanet.server.platform.tenantgame.api.TenantGameApi;
import com.tchalanet.server.platform.tenantgame.api.model.request.EnsureTenantGamesRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Tenant provisioning orchestrator (dashboard-overview-runtime-v1 §tenant-provisioning).
 *
 * Hard rules:
 *   - calls owning domain APIs only (no direct INSERTs into other domains)
 *   - never copies transactional data
 *   - V1 supports MINIMAL / DEFAULT_HAITI_LOTTERY / DEMO profiles only
 *   - result includes a freshly-computed TenantReadinessView
 */
@Service
@RequiredArgsConstructor
public class TenantProvisioningOrchestrator {

  private final TenantConfigApi tenantConfigApi;
  private final TenantPersistenceAdapter tenantPersistence;
  private final TenantReadinessAssembler readinessAssembler;
  private final IdentityApi identityApi;
  private final TenantGameApi tenantGameApi;
  private final PricingProvisioningApi pricingProvisioningApi;
  private final DrawChannelProvisioningApi drawChannelProvisioningApi;
  private final CommandBus commandBus;
  private final JsonUtils jsonUtils;

  public TenantProvisioningPreviewView preview(TenantProvisioningRequest request) {
    return new TenantProvisioningPreviewView(
        request.profile(),
        includedDomains(request.profile()),
        warnings(request),
        notCopiedData(),
        expectedReadinessSections(request.profile()));
  }

  public TenantProvisioningResultView provision(TenantProvisioningRequest request) {
    tenantConfigApi.createTenant(new CreateTenantRequest(
        request.code(),
        request.name(),
        request.type(),
        request.timezone(),
        request.currency(),
        null,
        null,
        Boolean.FALSE));

    var created = tenantConfigApi.getTenantByCode(
        new GetTenantByCodeRequest(request.code()));

    //todo pass dans create tenant
    // Persist the default commission rate on the freshly-created tenant. The column + adapter
    // already exist (tenant-admin commission flow); the create command does not carry it.
    tenantPersistence.updateDefaultCommissionRate(
        created.tenantId(), request.defaultCommissionRate());

    TchContextScope.runStartupTenant(
        created.tenantId().value(),
        "tenant-provisioning-catalog",
        () -> ensureProfileCatalog(created.tenantId(), request.profile()));

    Map<String, String> domainStatuses = new LinkedHashMap<>();
    domainStatuses.put("tenant_identity", "CREATED");
    domainStatuses.put("pagemodels", "SEEDED_VIA_LISTENER");
    domainStatuses.put("theme", "DEFAULT");
    domainStatuses.put("settings", "DEFAULT");
    domainStatuses.put("i18n", "DEFAULT");
    domainStatuses.put("games", profileGamesStatus(request.profile()));
    domainStatuses.put("pricing", profilePricingStatus(request.profile()));
    domainStatuses.put("draw_channels", profileDrawChannelsStatus(request.profile()));
    domainStatuses.put("promotions_templates", "TEMPLATES_AVAILABLE");
    domainStatuses.put("limits_templates", profileLimitStatus(request.profile()));

    UUID tenantId = created.tenantId().value();
    final String[] initialAdminUserId = {null};
    final String[] initialAdminCredentialStatus = {null};
    final String[] initialAdminTemporaryPassword = {null};
    final Boolean[] initialAdminMustChangePassword = {null};
    final Boolean[] initialAdminMustCompleteProfile = {null};

    // Run in new tenant context for RLS — creates admin + computes readiness.
    TenantReadinessView readiness = TchContextScope.runStartupTenantResult(
        tenantId,
        "tenant-provisioning",
        () -> {
          if (request.initialAdminEmail() != null && !request.initialAdminEmail().isBlank()) {
            var adminResult = identityApi.createTenantUser(
                TenantId.of(tenantId),
                created.code(),
                request.initialAdminEmail(),
                null,
                null,
                TchRole.TENANT_ADMIN);
            initialAdminUserId[0] = adminResult.userId().value().toString();
            initialAdminCredentialStatus[0] = adminResult.temporaryCredentialIssued()
                ? "TEMPORARY_PASSWORD_ISSUED"
                : (adminResult.created() ? "TEMPORARY_CREDENTIAL_NOT_RETURNED" : "EXISTING_USER_ATTACHED");
            initialAdminTemporaryPassword[0] = adminResult.temporaryPassword();
            initialAdminMustChangePassword[0] = Boolean.TRUE;
            initialAdminMustCompleteProfile[0] = Boolean.TRUE;
          }
          return readinessAssembler.assemble(TchContext.currentOrNull());
        });

    return new TenantProvisioningResultView(
        tenantId.toString(),
        created.code(),
        request.profile(),
        request.defaultCommissionRate(),
        Map.copyOf(domainStatuses),
        nextSteps(request.profile(), request.initialAdminEmail()),
        warnings(request),
        readiness,
        initialAdminUserId[0],
        blankToNull(request.initialAdminEmail()),
        initialAdminCredentialStatus[0],
        initialAdminTemporaryPassword[0],
        initialAdminMustChangePassword[0],
        initialAdminMustCompleteProfile[0]);
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
    if (request.initialAdminEmail() == null || request.initialAdminEmail().isBlank()) {
      return List.of("INITIAL_ADMIN_EMAIL_MISSING");
    }
    return List.of();
  }

  private static List<String> notCopiedData() {
    return List.of(
        "tickets", "sales", "payouts", "audit", "notifications", "stats", "ledger");
  }

  private static List<String> expectedReadinessSections(TenantProvisioningProfile profile) {
    return switch (profile) {
      case MINIMAL -> List.of(
          "identity", "users", "seller_terminals", "draw_channels", "seller_rules", "limits", "odds");
      case DEFAULT_HAITI_LOTTERY -> List.of(
          "identity", "users", "seller_terminals", "draw_channels", "seller_rules", "limits", "odds");
      case DEMO -> List.of("identity");
    };
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
    pricingProvisioningApi.ensureDefaultHaitiLotteryOdds(tenantId);
    drawChannelProvisioningApi.ensureDefaultHaitiLotteryChannels(
        tenantId,
        tenantGameApi.listGames(tenantId).stream()
            .map(game -> new ProvisioningTenantGameRef(game.tenantGameId(), game.gameCode()))
            .toList());
    ensureDefaultLimits(tenantId);
  }

  private static List<String> defaultHaitiLotteryGameCodes() {
    return List.of(
        "HT_BOLET",
        "HT_NUMERO",
        "HT_MARYAJ",
        "HT_MARYAJ_GRATUIT",
        "HT_LOTO3",
        "HT_LOTO4",
        "HT_LOTO5");
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
}
