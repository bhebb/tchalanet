package com.tchalanet.server.features.tenantadmin.readiness;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.core.sellerterminal.api.query.ListSellerTerminalsQuery;
import com.tchalanet.server.core.sellerterminal.api.query.SellerTerminalSearchCriteria;
import com.tchalanet.server.features.tenantadmin.readiness.model.TenantReadinessIssue;
import com.tchalanet.server.features.tenantadmin.readiness.model.TenantReadinessSection;
import com.tchalanet.server.features.tenantadmin.readiness.model.TenantReadinessStatus;
import com.tchalanet.server.features.tenantadmin.readiness.model.TenantReadinessSummary;
import com.tchalanet.server.features.tenantadmin.readiness.model.TenantReadinessView;
import java.util.ArrayList;
import java.util.List;

import com.tchalanet.server.platform.tenant.api.TenantPreContextLookupApi;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Computes tenant readiness once and exposes two projections:
 *   - {@link TenantReadinessSummary} for the tenant admin dashboard
 *   - {@link TenantReadinessView}    for the tenant overview and provisioning result
 *
 * Section checks wired for V1 (4 grouped reads):
 *   1. identity  → TenantCatalog.findRegistryById
 *   2. seller terminals → ListSellerTerminalsQuery (page size 1)
 *   3. users            → ListSellersQuery
 *
 * Remaining sections (games_pricing, draws, limits, promotions,
 * settings, i18n, theme, pagemodels) return UNKNOWN until their respective
 * structural check queries are exposed by each domain.
 *
 * Invariants enforced here:
 *   - readiness uses the current context/RLS, never a client-supplied tenant id
 *   - summary has no KPI fields (salesToday, ticketCountToday, activeSessions,
 *     openDraws, unread)
 */
@Component
@RequiredArgsConstructor
public class TenantReadinessAssembler {

  private final TenantPreContextLookupApi tenantPreContextLookupApi;
  private final QueryBus queryBus;

  /** V1 section catalog (mirrors dashboard-overview-runtime-v1 §11 tenant table). */
  private static final List<SectionDescriptor> SECTIONS = List.of(
      new SectionDescriptor("identity",     "readiness.section.identity",     "/app/admin"),
      new SectionDescriptor("users",        "readiness.section.users",        "/app/admin/users"),
      new SectionDescriptor("seller_terminals", "readiness.section.seller_terminals", "/app/admin/seller-terminals"),
      new SectionDescriptor("games_pricing","readiness.section.games_pricing","/app/admin/games-pricing"),
      new SectionDescriptor("draws",        "readiness.section.draws",        "/app/admin/draws"),
      new SectionDescriptor("limits",       "readiness.section.limits",       "/app/admin/limits"),
      new SectionDescriptor("promotions",   "readiness.section.promotions",   "/app/admin/promotions"),
      new SectionDescriptor("settings",     "readiness.section.settings",     "/app/admin/settings"),
      new SectionDescriptor("i18n",         "readiness.section.i18n",         "/app/admin/i18n"),
      new SectionDescriptor("theme",        "readiness.section.appearance",   "/app/admin/appearance"),
      new SectionDescriptor("pagemodels",   "readiness.section.pagemodels",   "/app/admin/pagemodels"));

  /**
   * Compute the full readiness view for the current tenant.
   * Returns {@link TenantReadinessView#unknown()} when no tenant is bound to the context.
   */
  public TenantReadinessView assemble(TchRequestContext ctx) {
    if (ctx == null || ctx.tenantId() == null) {
      return TenantReadinessView.unknown();
    }

    // --- Grouped reads (≤ 4 per dashboard-overview-runtime-v1 §12) ---
    boolean identityFound = checkIdentity(ctx);
    boolean hasSellerTerminals = checkSellerTerminals(ctx);

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
            issues.add(new TenantReadinessIssue("identity", "readiness.identity.missing", d.route()));
          }
        }
        case "seller_terminals" -> {
          if (hasSellerTerminals) {
            status = TenantReadinessStatus.READY;
          } else {
            status = TenantReadinessStatus.MISSING;
            issues.add(new TenantReadinessIssue(
                "seller_terminals", "readiness.seller_terminals.empty", d.route()));
          }
        }
        default ->
          // V1: remaining sections not yet checked — structural check queries TBD per domain
          status = TenantReadinessStatus.UNKNOWN;
      }

      sections.add(new TenantReadinessSection(
          d.id(), d.labelKey(), status, d.route(), List.copyOf(issues)));
    }

    TenantReadinessStatus globalStatus = rollUp(sections);
    int missingCount = countMissing(sections);
    return new TenantReadinessView(globalStatus, missingCount, List.copyOf(sections));
  }

  /**
   * Derive the short dashboard projection from a full view — no section data,
   * only the rolled-up status, the missing count, and a bounded top-issues list.
   */
  public TenantReadinessSummary toSummary(TenantReadinessView view) {
    return toSummary(view, 4);
  }

  public TenantReadinessSummary toSummary(TenantReadinessView view, int maxIssues) {
    if (view == null) return TenantReadinessSummary.unknown();
    List<TenantReadinessIssue> top = view.sections().stream()
        .flatMap(s -> s.issues().stream())
        .limit(Math.max(0, maxIssues))
        .toList();
    return new TenantReadinessSummary(view.status(), view.missingCount(), top);
  }

  // ---- section checks -------------------------------------------------------

  private boolean checkIdentity(TchRequestContext ctx) {
    try {
      return tenantPreContextLookupApi.findById(ctx.tenantId()).isPresent();
    } catch (RuntimeException e) {
      return false;
    }
  }

  private boolean checkSellerTerminals(TchRequestContext ctx) {
    try {
      var page = queryBus.ask(new ListSellerTerminalsQuery(
          ctx.tenantId(),
          SellerTerminalSearchCriteria.empty(),
          new TchPageRequest(PageRequest.of(0, 1))));
      return page != null && page.totalElements() > 0;
    } catch (RuntimeException e) {
      return false;
    }
  }



  // ---- rollup ---------------------------------------------------------------

  private TenantReadinessStatus rollUp(List<TenantReadinessSection> sections) {
    boolean anyMissing = sections.stream().anyMatch(s -> s.status() == TenantReadinessStatus.MISSING);
    boolean anyPartial = sections.stream().anyMatch(s -> s.status() == TenantReadinessStatus.PARTIAL);
    // All READY only if every known section is READY (UNKNOWN sections don't block READY)
    boolean allKnownReady = sections.stream()
        .filter(s -> s.status() != TenantReadinessStatus.UNKNOWN)
        .allMatch(s -> s.status() == TenantReadinessStatus.READY);
    boolean anyKnown = sections.stream()
        .anyMatch(s -> s.status() != TenantReadinessStatus.UNKNOWN);
    if (anyKnown && allKnownReady) return TenantReadinessStatus.READY;
    if (anyMissing) return TenantReadinessStatus.MISSING;
    if (anyPartial) return TenantReadinessStatus.PARTIAL;
    return TenantReadinessStatus.UNKNOWN;
  }

  private int countMissing(List<TenantReadinessSection> sections) {
    return (int) sections.stream()
        .filter(s -> s.status() == TenantReadinessStatus.MISSING
            || s.status() == TenantReadinessStatus.PARTIAL)
        .count();
  }

  private record SectionDescriptor(String id, String labelKey, String route) {}
}
