package com.tchalanet.server.features.tenantadmin.readiness;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.features.tenantadmin.readiness.model.TenantReadinessIssue;
import com.tchalanet.server.features.tenantadmin.readiness.model.TenantReadinessSection;
import com.tchalanet.server.features.tenantadmin.readiness.model.TenantReadinessStatus;
import com.tchalanet.server.features.tenantadmin.readiness.model.TenantReadinessSummary;
import com.tchalanet.server.features.tenantadmin.readiness.model.TenantReadinessView;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Computes tenant readiness once and exposes two projections:
 *   - {@link TenantReadinessSummary} for the tenant admin dashboard
 *   - {@link TenantReadinessView}    for the tenant overview and provisioning result
 *
 * The shape is final per dashboard-overview-runtime-v1. The per-section status
 * resolution is V1 best-effort and will be backfilled with real counts as each
 * domain exposes a structural check query (outlets, terminals, sellers, games,
 * pricing, draws, settings, i18n, theme, limits, promotions, pagemodels).
 *
 * Invariants enforced here:
 *   - readiness uses the current context/RLS, never a client-supplied tenant id
 *   - summary has no KPI fields (salesToday, ticketCountToday, activeSessions,
 *     openDraws, unread)
 */
@Component
public class TenantReadinessAssembler {

  /** V1 section catalog (mirrors dashboard-overview-runtime-v1 §11 tenant table). */
  private static final List<SectionDescriptor> SECTIONS = List.of(
      new SectionDescriptor("identity", "readiness.section.identity", "/app/admin"),
      new SectionDescriptor("users", "readiness.section.users", "/app/admin/users"),
      new SectionDescriptor("outlets", "readiness.section.outlets", "/app/admin/outlets"),
      new SectionDescriptor("terminals", "readiness.section.terminals", "/app/admin/terminals"),
      new SectionDescriptor("sessions", "readiness.section.sessions", "/app/admin/sessions"),
      new SectionDescriptor("games_pricing", "readiness.section.games_pricing", "/app/admin/games-pricing"),
      new SectionDescriptor("draws", "readiness.section.draws", "/app/admin/draws"),
      new SectionDescriptor("limits", "readiness.section.limits", "/app/admin/limits"),
      new SectionDescriptor("promotions", "readiness.section.promotions", "/app/admin/promotions"),
      new SectionDescriptor("settings", "readiness.section.settings", "/app/admin/settings"),
      new SectionDescriptor("i18n", "readiness.section.i18n", "/app/admin/i18n"),
      new SectionDescriptor("theme", "readiness.section.appearance", "/app/admin/appearance"),
      new SectionDescriptor("pagemodels", "readiness.section.pagemodels", "/app/admin/pagemodels"));

  /**
   * Compute the full readiness view for the current tenant.
   * Returns {@link TenantReadinessView#unknown()} when no tenant is bound to the context.
   */
  public TenantReadinessView assemble(TchRequestContext ctx) {
    if (ctx == null || ctx.tenantId() == null) {
      return TenantReadinessView.unknown();
    }

    List<TenantReadinessSection> sections = new ArrayList<>(SECTIONS.size());
    for (SectionDescriptor d : SECTIONS) {
      // V1: status is UNKNOWN by default. Specific section checks are added
      // incrementally; the shape stays stable for downstream consumers.
      TenantReadinessStatus sectionStatus = TenantReadinessStatus.UNKNOWN;
      List<TenantReadinessIssue> issues = List.of();
      sections.add(new TenantReadinessSection(
          d.id(), d.labelKey(), sectionStatus, d.route(), issues));
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

  private TenantReadinessStatus rollUp(List<TenantReadinessSection> sections) {
    boolean anyMissing = sections.stream().anyMatch(s -> s.status() == TenantReadinessStatus.MISSING);
    boolean anyPartial = sections.stream().anyMatch(s -> s.status() == TenantReadinessStatus.PARTIAL);
    boolean allReady = !sections.isEmpty()
        && sections.stream().allMatch(s -> s.status() == TenantReadinessStatus.READY);
    if (allReady) return TenantReadinessStatus.READY;
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
