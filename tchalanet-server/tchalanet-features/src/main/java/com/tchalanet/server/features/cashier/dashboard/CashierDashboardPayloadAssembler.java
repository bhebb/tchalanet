package com.tchalanet.server.features.cashier.dashboard;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.core.draw.api.query.ListCashierNextDrawsQuery;
import com.tchalanet.server.core.sales.api.query.GetCashierDashboardOverviewQuery;
import com.tchalanet.server.core.sales.api.query.ListCashierRecentTicketsQuery;
import com.tchalanet.server.core.session.api.query.GetCashierIdentityQuery;
import com.tchalanet.server.core.session.api.query.GetCashierSessionSummaryQuery;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Loads the grouped payload for source {@code cashier_dashboard}.
 * One assembly per request — memoized via {@code PageModelResolutionContext}.
 *
 * Grouped reads (target ≤ 4 per dashboard-overview-runtime-v1 §12):
 *   1. identity (GetCashierIdentityQuery)
 *   2. session  (GetCashierSessionSummaryQuery)
 *   3. overview (GetCashierDashboardOverviewQuery — only if session active)
 *   4. nextDraws (ListCashierNextDrawsQuery)
 *   5. recentTickets (ListCashierRecentTicketsQuery)
 */
@Component
@RequiredArgsConstructor
public class CashierDashboardPayloadAssembler {

  private static final int DEFAULT_NEXT_DRAWS_LIMIT = 8;
  private static final int DEFAULT_NEXT_DRAWS_LOOKAHEAD_HOURS = 48;
  private static final int DEFAULT_RECENT_TICKETS_LIMIT = 10;

  private final QueryBus queryBus;

  public Payload assemble(TchRequestContext ctx) {
    if (ctx == null || ctx.tenantId() == null || ctx.userId() == null) {
      return Payload.empty();
    }

    Map<String, Object> identity = loadIdentity(ctx);
    Map<String, Object> session = loadSession(ctx);
    Map<String, Object> overview = loadOverview(ctx, session);
    List<?> nextDraws = loadNextDraws();
    List<?> recentTickets = loadRecentTickets(ctx);

    return new Payload(identity, session, overview, nextDraws, recentTickets);
  }

  private Map<String, Object> loadIdentity(TchRequestContext ctx) {
    var view = queryBus.ask(new GetCashierIdentityQuery(ctx.tenantId(), ctx.userId()));
    if (view == null) return Map.of();
    var result = new HashMap<String, Object>();
    if (view.cashierDisplayName() != null) result.put("cashierDisplayName", view.cashierDisplayName());
    if (view.outletName() != null)         result.put("outletName", view.outletName());
    if (view.terminalLabel() != null)      result.put("terminalLabel", view.terminalLabel());
    if (view.tenantCode() != null)         result.put("tenantCode", view.tenantCode());
    return result;
  }

  private Map<String, Object> loadSession(TchRequestContext ctx) {
    var view = queryBus.ask(new GetCashierSessionSummaryQuery(ctx.tenantId(), ctx.userId()));
    if (view == null || !view.active()) {
      return Map.of("active", false);
    }
    return Map.of(
        "active", true,
        "sessionRef", view.sessionRef() != null ? view.sessionRef() : "",
        "openedAt", view.openedAt() != null ? view.openedAt().toString() : "",
        "openingFloatCents", view.openingFloatCents(),
        "salesTotalCents", view.salesTotalCents(),
        "ticketCount", view.ticketCount());
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> loadOverview(TchRequestContext ctx, Map<String, Object> session) {
    if (!Boolean.TRUE.equals(session.get("active"))) {
      return Map.of(
          "sessionOpen", false,
          "ticketCount", 0L,
          "salesTotalCents", 0L,
          "cancelledCount", 0L,
          "pendingApprovalCount", 0L);
    }

    String openedAtStr = (String) session.getOrDefault("openedAt", "");
    LocalDate businessDate = !openedAtStr.isBlank()
        ? java.time.Instant.parse(openedAtStr).atZone(ZoneOffset.UTC).toLocalDate()
        : LocalDate.now(ZoneOffset.UTC);

    var overview = queryBus.ask(
        new GetCashierDashboardOverviewQuery(ctx.tenantId(), ctx.userId(), businessDate));

    var result = new HashMap<String, Object>();
    result.put("sessionOpen", true);
    result.put("sessionRef", session.getOrDefault("sessionRef", ""));
    result.put("openedAt", openedAtStr);
    result.put("businessDate", businessDate.toString());
    result.put("ticketCount", overview != null ? overview.ticketCount() : 0L);
    result.put("salesTotalCents", overview != null ? overview.salesTotalCents() : 0L);
    result.put("cancelledCount", overview != null ? overview.cancelledCount() : 0L);
    result.put("pendingApprovalCount", overview != null ? overview.pendingApprovalCount() : 0L);
    result.put("byDraw", overview != null && overview.byDraw() != null ? overview.byDraw() : List.of());
    return result;
  }

  private List<?> loadNextDraws() {
    var items = queryBus.ask(
        new ListCashierNextDrawsQuery(DEFAULT_NEXT_DRAWS_LOOKAHEAD_HOURS, DEFAULT_NEXT_DRAWS_LIMIT));
    return items != null ? items : List.of();
  }

  private List<?> loadRecentTickets(TchRequestContext ctx) {
    var items = queryBus.ask(
        new ListCashierRecentTicketsQuery(ctx.userId(), DEFAULT_RECENT_TICKETS_LIMIT));
    return items != null ? items : List.of();
  }

  public record Payload(
      Map<String, Object> identity,
      Map<String, Object> session,
      Map<String, Object> overview,
      List<?> nextDraws,
      List<?> recentTickets) {

    public static Payload empty() {
      return new Payload(Map.of(), Map.of("active", false), Map.of(), List.of(), List.of());
    }
  }
}
