package com.tchalanet.server.features.pagemodel.dynamic.providers;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.core.pagemodel.api.model.PageModelDoc;
import com.tchalanet.server.core.sales.api.query.ListCashierPendingApprovalsQuery;
import com.tchalanet.server.core.pagemodel.api.dynamic.PageModelDynamicProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CashierPendingApprovalsProvider implements PageModelDynamicProvider {

  private final QueryBus queryBus;

  @Override
  public boolean supports(String logicalId, String widgetType, String source) {
    return "private.dashboard.cashier".equals(logicalId) && "cashier_pending_approvals".equals(source);
  }

  @Override
  public Object load(
      PageModelDoc pageModel,
      String widgetId,
      PageModelDoc.WidgetConfig widgetConfig,
      String lang,
      TchRequestContext ctx,
      com.tchalanet.server.core.pagemodel.api.dynamic.PageModelResolutionContext resolutionContext) {

    if (ctx == null || ctx.userId() == null) {
      return Map.of("count", 0, "items", List.of());
    }

    try {
      int limit = readInt(widgetConfig, "limit", 10);
      var items = queryBus.ask(new ListCashierPendingApprovalsQuery(ctx.userId(), limit));
      if (items == null) items = List.of();
      return Map.of("count", items.size(), "items", items);
    } catch (Exception e) {
      return Map.of("count", 0, "items", List.of());
    }
  }

  @Override
  public String providerKey() {
    return "cashier_pending_approvals";
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
