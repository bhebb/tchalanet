package com.tchalanet.server.features.pagemodel.dynamic.providers;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.core.pagemodel.internal.domain.model.PageModelDoc;
import com.tchalanet.server.core.sales.api.query.ListCashierTopSelectionsQuery;
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
public class CashierTopSelectionsProvider implements PageModelDynamicProvider {

  private final QueryBus queryBus;

  @Override
  public boolean supports(String logicalId, String widgetType, String source) {
    return "private.dashboard.cashier".equals(logicalId) && "cashier_top_selections".equals(source);
  }

  @Override
  public Object load(
      PageModelDoc pageModel,
      String widgetId,
      PageModelDoc.WidgetConfig widgetConfig,
      String lang,
      TchRequestContext ctx) {

    if (ctx == null || ctx.tenantId() == null || ctx.userId() == null) {
      return Map.of("byDraw", List.of());
    }

    try {
      int limitPerDraw = readInt(widgetConfig, "limit_per_draw", 5);

      LocalDate businessDate;
      try {
        var session = queryBus.ask(new GetCashierSessionSummaryQuery(ctx.tenantId(), ctx.userId()));
        businessDate = (session != null && session.active() && session.openedAt() != null)
            ? session.openedAt().atZone(ZoneOffset.UTC).toLocalDate()
            : LocalDate.now(ZoneOffset.UTC);
      } catch (Exception e) {
        businessDate = LocalDate.now(ZoneOffset.UTC);
      }

      var view = queryBus.ask(
          new ListCashierTopSelectionsQuery(ctx.userId(), businessDate, limitPerDraw));
      return view != null ? view : Map.of("byDraw", List.of());
    } catch (Exception e) {
      return Map.of("byDraw", List.of());
    }
  }

  @Override
  public String providerKey() {
    return "cashier_top_selections";
  }

  private static int readInt(PageModelDoc.WidgetConfig widgetConfig, String key, int defaultValue) {
    if (widgetConfig == null || widgetConfig.props() == null) return defaultValue;
    Object val = widgetConfig.props().get(key);
    if (val instanceof Number n) return n.intValue();
    if (val instanceof String s) {
      try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {}
    }
    return defaultValue;
  }
}
