package com.tchalanet.server.features.pagemodel.dynamic.providers;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.core.pagemodel.internal.domain.model.PageModelDoc;
import com.tchalanet.server.core.sales.api.query.GetCashierDashboardOverviewQuery;
import com.tchalanet.server.core.session.api.query.GetCashierSessionSummaryQuery;
import com.tchalanet.server.features.pagemodel.dynamic.PageModelDynamicProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CashierOverviewProvider implements PageModelDynamicProvider {

  private final QueryBus queryBus;

  @Override
  public boolean supports(String logicalId, String widgetType, String source) {
    return ("private.dashboard.cashier".equals(logicalId) || "private.dashboard.cashier.web".equals(logicalId))
        && "cashier_overview".equals(source);
  }

  @Override
  public Object load(
      PageModelDoc pageModel,
      String widgetId,
      PageModelDoc.WidgetConfig widgetConfig,
      String lang,
      TchRequestContext ctx) {

    if (ctx == null || ctx.tenantId() == null || ctx.userId() == null) {
      return fallback();
    }

    try {
      var session = queryBus.ask(new GetCashierSessionSummaryQuery(ctx.tenantId(), ctx.userId()));

      if (session == null || !session.active()) {
        return Map.of("sessionOpen", false, "ticketCount", 0L, "salesTotalCents", 0L,
            "cancelledCount", 0L, "pendingApprovalCount", 0L);
      }

      LocalDate businessDate = session.openedAt() != null
          ? session.openedAt().atZone(ZoneOffset.UTC).toLocalDate()
          : LocalDate.now(ZoneOffset.UTC);

      var overview = queryBus.ask(
          new GetCashierDashboardOverviewQuery(ctx.tenantId(), ctx.userId(), businessDate));

      var result = new java.util.LinkedHashMap<String, Object>();
      result.put("sessionOpen", true);
      result.put("sessionRef", session.sessionRef() != null ? session.sessionRef() : "");
      result.put("openedAt", session.openedAt() != null ? session.openedAt().toString() : "");
      result.put("businessDate", businessDate.toString());
      result.put("ticketCount", overview != null ? overview.ticketCount() : 0L);
      result.put("salesTotalCents", overview != null ? overview.salesTotalCents() : 0L);
      result.put("cancelledCount", overview != null ? overview.cancelledCount() : 0L);
      result.put("pendingApprovalCount", overview != null ? overview.pendingApprovalCount() : 0L);
      result.put("byDraw", overview != null && overview.byDraw() != null ? overview.byDraw() : List.of());
      return result;
    } catch (Exception e) {
      return fallback();
    }
  }

  private Map<String, Object> fallback() {
    return Map.of(
        "sessionOpen", false,
        "ticketCount", 0L,
        "salesTotalCents", 0L,
        "cancelledCount", 0L,
        "pendingApprovalCount", 0L);
  }

  @Override
  public String providerKey() {
    return "cashier_overview";
  }
}
