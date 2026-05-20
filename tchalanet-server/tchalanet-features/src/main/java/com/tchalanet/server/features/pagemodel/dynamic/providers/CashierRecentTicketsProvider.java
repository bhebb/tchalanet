package com.tchalanet.server.features.pagemodel.dynamic.providers;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.core.pagemodel.internal.domain.model.PageModelDoc;
import com.tchalanet.server.core.sales.api.query.ListCashierRecentTicketsQuery;
import com.tchalanet.server.features.pagemodel.dynamic.PageModelDynamicProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CashierRecentTicketsProvider implements PageModelDynamicProvider {

  private final QueryBus queryBus;

  @Override
  public boolean supports(String logicalId, String widgetType, String source) {
    return "private.dashboard.cashier".equals(logicalId) && "cashier_recent_tickets".equals(source);
  }

  @Override
  public Object load(
      PageModelDoc pageModel,
      String widgetId,
      PageModelDoc.WidgetConfig widgetConfig,
      String lang,
      TchRequestContext ctx) {

    if (ctx == null || ctx.userId() == null) {
      return Map.of("items", List.of());
    }

    try {
      int limit = readLimit(widgetConfig);
      var items = queryBus.ask(new ListCashierRecentTicketsQuery(ctx.userId(), limit));
      return Map.of("items", items != null ? items : List.of());
    } catch (Exception e) {
      return Map.of("items", List.of());
    }
  }

  @Override
  public String providerKey() {
    return "cashier_recent_tickets";
  }

  private static int readLimit(PageModelDoc.WidgetConfig widgetConfig) {
    if (widgetConfig == null || widgetConfig.props() == null) return 5;
    Object val = widgetConfig.props().get("limit");
    if (val instanceof Number n) return n.intValue();
    if (val instanceof String s) {
      try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {}
    }
    return 5;
  }
}
