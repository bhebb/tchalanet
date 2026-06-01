package com.tchalanet.server.features.platformadmin.tenantonboarding;

import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.common.context.TchContextScope;
import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.features.platformadmin.tenantonboarding.model.TenantProvisioningPreviewView;
import com.tchalanet.server.features.platformadmin.tenantonboarding.model.TenantProvisioningProfile;
import com.tchalanet.server.features.platformadmin.tenantonboarding.model.TenantProvisioningRequest;
import com.tchalanet.server.features.platformadmin.tenantonboarding.model.TenantProvisioningResultView;
import com.tchalanet.server.features.tenantadmin.readiness.TenantReadinessAssembler;
import com.tchalanet.server.features.tenantadmin.readiness.model.TenantReadinessView;
import com.tchalanet.server.platform.identity.api.IdentityApi;
import com.tchalanet.server.platform.tenantconfig.api.TenantConfigApi;
import com.tchalanet.server.platform.tenantconfig.api.model.request.CreateTenantRequest;
import com.tchalanet.server.platform.tenantconfig.api.model.request.GetTenantByCodeRequest;
import com.tchalanet.server.platform.tenantconfig.api.model.view.TenantConfigView;
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
  private final TenantReadinessAssembler readinessAssembler;
  private final IdentityApi identityApi;

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

    TenantConfigView created = tenantConfigApi.getTenantByCode(
        new GetTenantByCodeRequest(request.code()));

    Map<String, String> domainStatuses = new LinkedHashMap<>();
    domainStatuses.put("tenant_identity", "CREATED");
    domainStatuses.put("pagemodels", "SEEDED_VIA_LISTENER");
    domainStatuses.put("theme", "DEFAULT");
    domainStatuses.put("settings", "DEFAULT");
    domainStatuses.put("i18n", "DEFAULT");
    domainStatuses.put("games", profileGamesStatus(request.profile()));
    domainStatuses.put("pricing", profilePricingStatus(request.profile()));
    domainStatuses.put("draw_channels", profileDrawChannelsStatus(request.profile()));
    domainStatuses.put("promotions", "TEMPLATES_AVAILABLE");
    domainStatuses.put("limits", "TEMPLATES_AVAILABLE");

    UUID tenantId = created.tenantId().value();
    final String[] initialAdminUserId = {null};

    // Run in new tenant context for RLS — creates admin + computes readiness.
    TenantReadinessView readiness = TchContextScope.runStartupTenantResult(
        tenantId,
        "tenant-provisioning",
        () -> {
          if (request.initialAdminEmail() != null && !request.initialAdminEmail().isBlank()) {
            var adminResult = identityApi.createTenantUser(
                TenantId.of(tenantId),
                request.initialAdminEmail(),
                null,
                null,
                TchRole.TENANT_ADMIN);
            initialAdminUserId[0] = adminResult.userId().value().toString();
          }
          return readinessAssembler.assemble(TchContext.currentOrNull());
        });

    return new TenantProvisioningResultView(
        tenantId.toString(),
        created.code(),
        request.profile(),
        Map.copyOf(domainStatuses),
        nextSteps(request.profile(), request.initialAdminEmail()),
        warnings(request),
        readiness,
        initialAdminUserId[0]);
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
          "limits_templates", "demo_users", "demo_outlets", "demo_terminals");
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
        "tickets", "sales", "sessions", "payouts", "terminal_bindings",
        "audit", "notifications", "stats", "ledger", "offline_submissions");
  }

  private static List<String> expectedReadinessSections(TenantProvisioningProfile profile) {
    return switch (profile) {
      case MINIMAL -> List.of("identity", "users", "outlets", "terminals", "games_pricing", "draws");
      case DEFAULT_HAITI_LOTTERY -> List.of("identity", "users", "outlets", "terminals", "draws");
      case DEMO -> List.of("identity");
    };
  }

  private static List<String> nextSteps(TenantProvisioningProfile profile, String initialAdminEmail) {
    boolean adminCreated = initialAdminEmail != null && !initialAdminEmail.isBlank();
    return switch (profile) {
      case MINIMAL -> adminCreated
          ? List.of("CONFIGURE_GAMES", "CREATE_OUTLET", "CREATE_TERMINAL")
          : List.of("CREATE_INITIAL_ADMIN", "CONFIGURE_GAMES", "CREATE_OUTLET", "CREATE_TERMINAL");
      case DEFAULT_HAITI_LOTTERY -> adminCreated
          ? List.of("CREATE_OUTLET", "CREATE_TERMINAL")
          : List.of("CREATE_INITIAL_ADMIN", "CREATE_OUTLET", "CREATE_TERMINAL");
      case DEMO -> List.of("VERIFY_DEMO_SETUP");
    };
  }

  private static String profileGamesStatus(TenantProvisioningProfile profile) {
    return profile == TenantProvisioningProfile.MINIMAL ? "NONE" : "DEFAULT_LOTTERY";
  }

  private static String profilePricingStatus(TenantProvisioningProfile profile) {
    return profile == TenantProvisioningProfile.MINIMAL ? "NONE" : "DEFAULT";
  }

  private static String profileDrawChannelsStatus(TenantProvisioningProfile profile) {
    return profile == TenantProvisioningProfile.MINIMAL ? "NONE" : "DEFAULT_HAITI";
  }
}
