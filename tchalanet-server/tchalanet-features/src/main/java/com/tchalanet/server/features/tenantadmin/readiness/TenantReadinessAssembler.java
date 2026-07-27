package com.tchalanet.server.features.tenantadmin.readiness;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.core.draw.api.query.DrawSearchCriteria;
import com.tchalanet.server.core.draw.api.query.ListDrawsQuery;
import com.tchalanet.server.core.limitpolicy.api.query.LimitScopeQueryRef;
import com.tchalanet.server.core.limitpolicy.api.query.ListLimitAssignmentsByScopeQuery;
import com.tchalanet.server.core.limitpolicy.api.query.ListLimitAssignmentsView;
import com.tchalanet.server.core.promotion.api.query.ListPromotionCampaignsQuery;
import com.tchalanet.server.core.sellerterminal.api.query.ListSellerTerminalsQuery;
import com.tchalanet.server.core.sellerterminal.api.query.SellerTerminalSearchCriteria;
import com.tchalanet.server.core.subscription.api.query.ResolveTenantSubscriptionQuery;
import com.tchalanet.server.features.tenantadmin.overview.TenantSetupView;
import com.tchalanet.server.features.tenantadmin.readiness.model.TenantReadinessIssue;
import com.tchalanet.server.features.tenantadmin.readiness.model.TenantReadinessSection;
import com.tchalanet.server.features.tenantadmin.readiness.model.TenantReadinessStatus;
import com.tchalanet.server.features.tenantadmin.readiness.model.TenantReadinessSummary;
import com.tchalanet.server.features.tenantadmin.readiness.model.TenantReadinessView;
import com.tchalanet.server.features.tenantadmin.setup.TenantDrawSalesMatrixService;
import com.tchalanet.server.features.tenantadmin.setup.TenantGamesPricingService;
import com.tchalanet.server.features.tenantadmin.setup.model.TenantDrawSalesMatrixView;
import com.tchalanet.server.features.tenantadmin.setup.model.TenantGamesPricingView;
import com.tchalanet.server.platform.address.api.AddressApi;
import com.tchalanet.server.platform.tenant.api.TenantConfigApi;
import com.tchalanet.server.platform.tenant.api.TenantPreContextLookupApi;
import com.tchalanet.server.platform.tenant.api.model.request.GetTenantByIdRequest;
import com.tchalanet.server.platform.tenant.api.model.view.TenantSettingsReadinessView;
import com.tchalanet.server.platform.tenanttheme.api.TenantThemeApi;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Computes tenant readiness once and exposes two projections: - {@link TenantReadinessSummary} for
 * the tenant admin dashboard - {@link TenantReadinessView} for the tenant overview and provisioning
 * result
 *
 * <p>Section checks wired for V1: 1. identity → TenantCatalog.findRegistryById 2. address →
 * AddressApi.findPrimaryByTenantId 3. seller terminals → ListSellerTerminalsQuery (page size 1) 4.
 * games_pricing → TenantGamesPricingService — requires active games AND at least one of them with
 * odds/pricing configured (MISSING if none priced, PARTIAL if some active games still lack
 * pricing). Per-game limits are informational only, not part of this check. 5. draws →
 * TenantDrawSalesMatrixService (channel config + games×channel matrix) 6. generated_draws →
 * ListDrawsQuery (page size 1) — an actual Draw instance exists, not just configuration. This is a
 * setup blocker because the tenant must be able to generate a draw in its own context before it is
 * considered operational. 7. theme → TenantThemeApi active tenant theme 8. promotions →
 * ListPromotionCampaignsQuery (page size 1) 9. settings → TenantConfigApi settings readiness
 * service 10. subscription → ResolveTenantSubscriptionQuery — READY if ACTIVE/TRIAL, PARTIAL if any
 * other known status, MISSING if none applied. Visible section, not (yet) blocking. 11. commission
 * → TenantPreContextLookupApi default commission rate. Informational, not blocking. 12. limits →
 * ListLimitAssignmentsByScopeQuery, READY if at least one assignment is enabled. Informational, not
 * blocking.
 *
 * <p>Remaining sections (i18n, pageModels) return UNKNOWN until their respective structural check
 * queries are exposed.
 *
 * <p>Invariants enforced here: - readiness uses the current context/RLS, never a client-supplied
 * tenant id - summary has no KPI fields (salesToday, ticketCountToday, activeSessions, openDraws,
 * unread) - canCreateSellerTerminal requires identity + address, games_pricing, draws, and an
 * actual generated draw to be non-MISSING
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TenantReadinessAssembler {

  private static final Set<String> BLOCKING_SECTIONS =
      Set.of("identity", "address", "games_pricing", "draws", "generated_draws");

  private static final Set<String> NON_ROLLUP_SECTIONS =
      Set.of("subscription", "commission", "limits");

  /**
   * Required setup steps, grouped to match the admin setup checklist cards
   * (tenant-onboarding-console-rework-v1): identity + address are shown as one merged "Identité &
   * adresse" card, so they count as a single completed step, not two. generated_draws is its own
   * required step — configured games/channels aren't enough to sell without an actual generated
   * Draw instance to bet on. settings counts toward the progress percentage (its card is badged
   * "required") without blocking seller-terminal creation — it's not in BLOCKING_SECTIONS.
   */
  private static final List<List<String>> REQUIRED_STEP_GROUPS =
      List.of(
          List.of("identity", "address"),
          List.of("settings"),
          List.of("games_pricing"),
          List.of("draws"),
          List.of("generated_draws"));

  private final TenantPreContextLookupApi tenantPreContextLookupApi;
  private final AddressApi addressApi;
  private final TenantConfigApi tenantConfigApi;
  private final QueryBus queryBus;
  private final TenantGamesPricingService gamesPricingService;
  private final TenantDrawSalesMatrixService drawSalesMatrixService;
  private final TenantThemeApi tenantThemeApi;

  /** V1 section catalog (mirrors dashboard-overview-runtime-v1 §11 tenant table). */
  private static final List<SectionDescriptor> SECTIONS =
      List.of(
          new SectionDescriptor("identity", "readiness.section.identity", "/app/admin"),
          new SectionDescriptor("address", "readiness.section.address", "/app/admin/onboarding"),
          new SectionDescriptor("users", "readiness.section.users", "/app/admin/users"),
          new SectionDescriptor(
              "seller_terminals",
              "readiness.section.seller_terminals",
              "/app/admin/seller-terminals"),
          new SectionDescriptor(
              "games_pricing", "readiness.section.games_pricing", "/app/admin/games-pricing"),
          new SectionDescriptor("draws", "readiness.section.draws", "/app/admin/draws"),
          new SectionDescriptor(
              "generated_draws", "readiness.section.generated_draws", "/app/admin/draws"),
          new SectionDescriptor("limits", "readiness.section.limits", "/app/admin/limits"),
          new SectionDescriptor(
              "commission", "readiness.section.commission", "/app/admin/controls/commissions"),
          new SectionDescriptor(
              "promotions", "readiness.section.promotions", "/app/admin/promotions"),
          new SectionDescriptor("settings", "readiness.section.settings", "/app/admin/settings"),
          new SectionDescriptor(
              "subscription", "readiness.section.subscription", "/app/admin/subscription"),
          new SectionDescriptor("i18n", "readiness.section.i18n", "/app/admin/i18n"),
          new SectionDescriptor("theme", "readiness.section.appearance", "/app/admin/appearance"),
          new SectionDescriptor(
              "pagemodels", "readiness.section.pagemodels", "/app/admin/pagemodels"));

  /**
   * Compute the full readiness view for the current tenant. Returns {@link
   * TenantReadinessView#unknown()} when no tenant is bound to the context.
   */
  public TenantReadinessView assemble(TchRequestContext ctx) {
    if (ctx == null || ctx.effectiveTenantIdOrNull() == null) {
      return TenantReadinessView.unknown();
    }

    boolean identityFound = checkIdentity(ctx);
    boolean hasAddress = checkAddress(ctx);
    boolean hasSellerTerminals = checkSellerTerminals(ctx);
    var gamesPricingStatus = checkGamesPricing(ctx);
    var drawsStatus = checkDraws(ctx);
    var generatedDrawsStatus = checkGeneratedDraws(ctx);
    var themeStatus = checkTheme(ctx);
    var promotionsStatus = checkPromotions(ctx);
    var settingsReadiness = checkSettings(ctx);
    var subscriptionStatus = checkSubscription(ctx);
    var commissionStatus = checkCommission(ctx);
    var limitsStatus = checkLimits(ctx);

    List<TenantReadinessSection> sections = new ArrayList<>(SECTIONS.size());
    for (SectionDescriptor d : SECTIONS) {
      TenantReadinessStatus status;
      List<TenantReadinessIssue> issues = new ArrayList<>();

      switch (d.id()) {
        case "identity" -> {
          if (identityFound) {
            status = TenantReadinessStatus.READY;
          } else {
            status = TenantReadinessStatus.MISSING;
            issues.add(
                new TenantReadinessIssue("identity", "readiness.identity.missing", d.route()));
          }
        }
        case "address" -> {
          if (hasAddress) {
            status = TenantReadinessStatus.READY;
          } else {
            status = TenantReadinessStatus.MISSING;
            issues.add(new TenantReadinessIssue("address", "readiness.address.missing", d.route()));
          }
        }
        case "seller_terminals" -> {
          if (hasSellerTerminals) {
            status = TenantReadinessStatus.READY;
          } else {
            status = TenantReadinessStatus.MISSING;
            issues.add(
                new TenantReadinessIssue(
                    "seller_terminals", "readiness.seller_terminals.empty", d.route()));
          }
        }
        case "games_pricing" -> {
          status = gamesPricingStatus;
          if (status == TenantReadinessStatus.MISSING) {
            issues.add(
                new TenantReadinessIssue(
                    "games_pricing", "readiness.games_pricing.missing", d.route()));
          } else if (status == TenantReadinessStatus.PARTIAL) {
            issues.add(
                new TenantReadinessIssue(
                    "games_pricing", "readiness.games_pricing.partial", d.route()));
          }
        }
        case "draws" -> {
          status = drawsStatus;
          if (status == TenantReadinessStatus.MISSING) {
            issues.add(new TenantReadinessIssue("draws", "readiness.draws.missing", d.route()));
          } else if (status == TenantReadinessStatus.PARTIAL) {
            issues.add(new TenantReadinessIssue("draws", "readiness.draws.partial", d.route()));
          }
        }
        case "generated_draws" -> {
          status = generatedDrawsStatus;
          if (status == TenantReadinessStatus.MISSING) {
            issues.add(
                new TenantReadinessIssue(
                    "generated_draws", "readiness.generated_draws.missing", d.route()));
          }
        }
        case "theme" -> status = themeStatus;
        case "promotions" -> status = promotionsStatus;
        case "settings" -> {
          if (settingsReadiness == null) {
            status = TenantReadinessStatus.UNKNOWN;
          } else if (settingsReadiness.ready()) {
            status = TenantReadinessStatus.READY;
          } else {
            status = TenantReadinessStatus.MISSING;
            var reasonCodes =
                settingsReadiness.sections().stream()
                    .flatMap(section -> section.blockingReasons().stream())
                    .toList();
            if (reasonCodes.isEmpty()) {
              issues.add(
                  new TenantReadinessIssue("settings", "readiness.settings.missing", d.route()));
            } else {
              reasonCodes.forEach(
                  reason -> issues.add(new TenantReadinessIssue("settings", reason, d.route())));
            }
          }
        }
        case "subscription" -> {
          status = subscriptionStatus;
          if (status == TenantReadinessStatus.MISSING) {
            issues.add(
                new TenantReadinessIssue(
                    "subscription", "readiness.subscription.missing", d.route()));
          }
        }
        case "commission" -> {
          status = commissionStatus;
          if (status == TenantReadinessStatus.MISSING) {
            issues.add(
                new TenantReadinessIssue(
                    "commission", "readiness.commission.default_missing", d.route()));
          }
        }
        case "limits" -> {
          status = limitsStatus;
          if (status == TenantReadinessStatus.MISSING) {
            issues.add(new TenantReadinessIssue("limits", "readiness.limits.empty", d.route()));
          }
        }
        default ->
            // V1: remaining sections not yet checked — structural check queries TBD per domain
            status = TenantReadinessStatus.UNKNOWN;
      }

      sections.add(
          new TenantReadinessSection(d.id(), d.labelKey(), status, d.route(), List.copyOf(issues)));
    }

    TenantReadinessStatus globalStatus = rollUp(sections);
    int missingCount = countMissing(sections);
    return new TenantReadinessView(globalStatus, missingCount, List.copyOf(sections));
  }

  /**
   * Compute the setup progression view from the readiness sections. Covers the required setup steps
   * shown on the admin setup page (excludes optional sections such as theme/promotions and the
   * seller terminal CTA unlocked by canCreateSellerTerminal).
   */
  public TenantSetupView computeSetup(TenantReadinessView view) {
    if (view == null) return TenantSetupView.unknown();

    List<TenantReadinessSection> sections = view.sections();
    Map<String, TenantReadinessStatus> statusById =
        sections.stream()
            .collect(Collectors.toMap(TenantReadinessSection::id, TenantReadinessSection::status));

    // One step per required group — identity+address only counts once both are READY.
    int total = REQUIRED_STEP_GROUPS.size();
    int completed =
        (int)
            REQUIRED_STEP_GROUPS.stream()
                .filter(
                    group ->
                        group.stream()
                            .allMatch(id -> statusById.get(id) == TenantReadinessStatus.READY))
                .count();

    // Blocking steps: sections required for canCreateSellerTerminal that are explicitly MISSING
    List<String> blocking =
        sections.stream()
            .filter(s -> BLOCKING_SECTIONS.contains(s.id()))
            .filter(s -> s.status() == TenantReadinessStatus.MISSING)
            .map(s -> s.id().toUpperCase())
            .toList();

    boolean canCreate = blocking.isEmpty();

    var next =
        REQUIRED_STEP_GROUPS.stream()
            .flatMap(List::stream)
            .filter(id -> statusById.get(id) != TenantReadinessStatus.READY)
            .map(String::toUpperCase)
            .findFirst()
            .orElse(null);

    var status = completed >= total ? "COMPLETE" : "INCOMPLETE";
    return new TenantSetupView(total, completed, status, canCreate, blocking, next);
  }

  /**
   * Derive the short dashboard projection from a full view — no section data, only the rolled-up
   * status, the missing count, and a bounded top-issues list.
   */
  public TenantReadinessSummary toSummary(TenantReadinessView view) {
    return toSummary(view, 4);
  }

  public TenantReadinessSummary toSummary(TenantReadinessView view, int maxIssues) {
    if (view == null) return TenantReadinessSummary.unknown();
    List<TenantReadinessIssue> top =
        view.sections().stream()
            .flatMap(s -> s.issues().stream())
            .limit(Math.max(0, maxIssues))
            .toList();
    return new TenantReadinessSummary(view.status(), view.missingCount(), top);
  }

  // ---- section checks -------------------------------------------------------

  private boolean checkIdentity(TchRequestContext ctx) {
    try {
      return tenantPreContextLookupApi.findById(ctx.tenantIdRequired()).isPresent();
    } catch (RuntimeException e) {
      log.warn("Failed to check tenant identity for readiness: {}", e.getMessage(), e);
    }
    return false;
  }

  private boolean checkAddress(TchRequestContext ctx) {
    try {
      return addressApi.findPrimaryByTenantId(ctx.tenantIdRequired()).isPresent();
    } catch (RuntimeException e) {
      log.warn("Failed to check tenant address for readiness: {}", e.getMessage(), e);
    }
    return false;
  }

  private boolean checkSellerTerminals(TchRequestContext ctx) {
    try {
      var page =
          queryBus.ask(
              new ListSellerTerminalsQuery(
                  ctx.tenantIdRequired(),
                  SellerTerminalSearchCriteria.empty(),
                  new TchPageRequest(PageRequest.of(0, 1))));
      return page != null && page.totalElements() > 0;
    } catch (RuntimeException e) {
      log.warn("Failed to check tenant seller terminals for readiness: {}", e.getMessage(), e);
    }
    return false;
  }

  private TenantReadinessStatus checkGamesPricing(TchRequestContext ctx) {
    try {
      TenantGamesPricingView view = gamesPricingService.get(ctx.tenantIdRequired());
      if (view == null || view.games() == null || view.games().isEmpty()) {
        return TenantReadinessStatus.MISSING;
      }

      var activeGames = view.games().stream().filter(g -> g.enabled() && g.visibleInPos()).toList();
      if (activeGames.isEmpty()) {
        return TenantReadinessStatus.MISSING;
      }

      // Odds/pricing must be configured to actually sell — an "active" game with no
      // priced entries isn't sellable. limits are informational only (not checked here).
      long pricedGames =
          activeGames.stream().filter(g -> g.pricing() != null && g.pricing().configured()).count();
      if (pricedGames == 0) {
        return TenantReadinessStatus.MISSING;
      }
      if (pricedGames < activeGames.size()) {
        return TenantReadinessStatus.PARTIAL;
      }

      return TenantReadinessStatus.READY;
    } catch (RuntimeException e) {
      log.warn("Failed to check tenant games pricing for readiness: {}", e.getMessage(), e);
    }
    return TenantReadinessStatus.UNKNOWN;
  }

  private TenantReadinessStatus checkDraws(TchRequestContext ctx) {
    try {
      TenantDrawSalesMatrixView view = drawSalesMatrixService.get(ctx.tenantIdRequired());
      if (view == null || view.summary() == null) {
        return TenantReadinessStatus.MISSING;
      }

      var summary = view.summary();
      if (summary.configuredChannelCount() == 0 || summary.offeredChannelGameCount() == 0) {
        return TenantReadinessStatus.MISSING;
      }
      if (summary.activeChannelCount() == 0) {
        return TenantReadinessStatus.PARTIAL;
      }
      return TenantReadinessStatus.READY;
    } catch (RuntimeException e) {
      log.warn("Failed to check tenant Draws for readiness: {}", e.getMessage(), e);
    }
    return TenantReadinessStatus.UNKNOWN;
  }

  /**
   * Whether at least one Draw instance has actually been generated (not just configured). Required:
   * configured channels/games aren't enough to sell without a real Draw to bet on.
   */
  private TenantReadinessStatus checkGeneratedDraws(TchRequestContext ctx) {
    try {
      var page =
          queryBus.ask(
              new ListDrawsQuery(
                  DrawSearchCriteria.of(null, null, null, null), PageRequest.of(0, 1)));
      return page != null && page.totalElements() > 0
          ? TenantReadinessStatus.READY
          : TenantReadinessStatus.MISSING;
    } catch (RuntimeException e) {
      log.warn("Failed  to check tenant generated Draws for readiness: {}", e.getMessage(), e);
    }

    return TenantReadinessStatus.UNKNOWN;
  }

  /** Whether the tenant has an active (or trialing) subscription/plan. */
  private TenantReadinessStatus checkSubscription(TchRequestContext ctx) {
    try {
      var subscription = queryBus.ask(new ResolveTenantSubscriptionQuery(ctx.tenantIdRequired()));
      if (subscription == null) {
        return TenantReadinessStatus.MISSING;
      }
      String statusName = subscription.status() == null ? null : subscription.status().name();
      if ("ACTIVE".equals(statusName) || "TRIAL".equals(statusName)) {
        return TenantReadinessStatus.READY;
      }
      return TenantReadinessStatus.PARTIAL;
    } catch (RuntimeException e) {
      log.warn("Failed to check tenant subscription for readiness: {}", e.getMessage(), e);
    }
    return TenantReadinessStatus.UNKNOWN;
  }

  /** Whether the tenant has a default commission rate configured. */
  private TenantReadinessStatus checkCommission(TchRequestContext ctx) {
    try {
      boolean configured =
          tenantPreContextLookupApi
              .findById(ctx.tenantIdRequired())
              .flatMap(v -> v.defaultCommissionRate())
              .isPresent();
      return configured ? TenantReadinessStatus.READY : TenantReadinessStatus.MISSING;
    } catch (RuntimeException e) {
      log.warn("Failed to check tenant commission for readiness: {}", e.getMessage(), e);
    }
    return TenantReadinessStatus.UNKNOWN;
  }

  /** Whether the tenant has at least one enabled sales limit configured. */
  private TenantReadinessStatus checkLimits(TchRequestContext ctx) {
    try {
      ListLimitAssignmentsView assignments =
          queryBus.ask(
              new ListLimitAssignmentsByScopeQuery(
                  LimitScopeQueryRef.tenant(ctx.tenantIdRequired())));
      boolean hasEnabled =
          assignments != null
              && assignments.items().stream().anyMatch(ListLimitAssignmentsView.Item::enabled);
      return hasEnabled ? TenantReadinessStatus.READY : TenantReadinessStatus.MISSING;
    } catch (RuntimeException e) {
      log.warn("Failed to check tenant limits for readiness: {}", e.getMessage(), e);
    }
    return TenantReadinessStatus.UNKNOWN;
  }

  private TenantSettingsReadinessView checkSettings(TchRequestContext ctx) {
    try {
      return tenantConfigApi.getTenantSettingsReadiness(
          new GetTenantByIdRequest(ctx.tenantIdRequired()));
    } catch (RuntimeException e) {
      log.warn("Failed to check tenant settings for readiness: {}", e.getMessage(), e);
    }
    return null;
  }

  private TenantReadinessStatus checkTheme(TchRequestContext ctx) {
    try {
      return tenantThemeApi
              .findActiveTenantTheme(ctx.tenantIdRequired())
              .filter(
                  theme ->
                      theme.active() && theme.presetCode() != null && !theme.presetCode().isBlank())
              .isPresent()
          ? TenantReadinessStatus.READY
          : TenantReadinessStatus.UNKNOWN;
    } catch (RuntimeException e) {
      log.warn("Failed to check tenant theme for readiness: {}", e.getMessage(), e);
    }
    return TenantReadinessStatus.UNKNOWN;
  }

  private TenantReadinessStatus checkPromotions(TchRequestContext ctx) {
    try {
      var page = queryBus.ask(new ListPromotionCampaignsQuery(PageRequest.of(0, 1)));
      return page != null && page.totalElements() > 0
          ? TenantReadinessStatus.READY
          : TenantReadinessStatus.UNKNOWN;
    } catch (RuntimeException e) {
      log.warn("Failed to check tenant promotions for readiness: {}", e.getMessage(), e);
    }
    return TenantReadinessStatus.UNKNOWN;
  }

  // ---- rollup ---------------------------------------------------------------

  private TenantReadinessStatus rollUp(List<TenantReadinessSection> sections) {
    var rollupSections =
        sections.stream().filter(s -> !NON_ROLLUP_SECTIONS.contains(s.id())).toList();
    boolean anyMissing =
        rollupSections.stream().anyMatch(s -> s.status() == TenantReadinessStatus.MISSING);
    boolean anyPartial =
        rollupSections.stream().anyMatch(s -> s.status() == TenantReadinessStatus.PARTIAL);
    boolean allKnownReady =
        rollupSections.stream()
            .filter(s -> s.status() != TenantReadinessStatus.UNKNOWN)
            .allMatch(s -> s.status() == TenantReadinessStatus.READY);
    boolean anyKnown =
        rollupSections.stream().anyMatch(s -> s.status() != TenantReadinessStatus.UNKNOWN);
    if (anyKnown && allKnownReady) return TenantReadinessStatus.READY;
    if (anyMissing) return TenantReadinessStatus.MISSING;
    if (anyPartial) return TenantReadinessStatus.PARTIAL;
    return TenantReadinessStatus.UNKNOWN;
  }

  private int countMissing(List<TenantReadinessSection> sections) {
    return (int)
        sections.stream()
            .filter(s -> !NON_ROLLUP_SECTIONS.contains(s.id()))
            .filter(
                s ->
                    s.status() == TenantReadinessStatus.MISSING
                        || s.status() == TenantReadinessStatus.PARTIAL)
            .count();
  }

  private record SectionDescriptor(String id, String labelKey, String route) {}
}
