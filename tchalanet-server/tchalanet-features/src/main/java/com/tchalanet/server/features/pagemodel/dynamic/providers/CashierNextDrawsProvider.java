package com.tchalanet.server.features.pagemodel.dynamic.providers;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.core.draw.api.query.ListCashierNextDrawsQuery;
import com.tchalanet.server.core.pagemodel.api.model.PageModelDoc;
import com.tchalanet.server.core.pagemodel.api.dynamic.PageModelDynamicProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CashierNextDrawsProvider implements PageModelDynamicProvider {

  private final QueryBus queryBus;

  @Override
  public boolean supports(String logicalId, String widgetType, String source) {
    return ("private.dashboard.cashier".equals(logicalId) || "private.dashboard.cashier.web".equals(logicalId))
        && "cashier_next_draws".equals(source);
  }

  @Override
  public Object load(
      PageModelDoc pageModel,
      String widgetId,
      PageModelDoc.WidgetConfig widgetConfig,
      String lang,
      TchRequestContext ctx,
      com.tchalanet.server.core.pagemodel.api.dynamic.PageModelResolutionContext resolutionContext) {

    try {
      var props = widgetConfig == null ? null : widgetConfig.props();
      int limit = readInt(props, "limit", 8);
      int lookaheadHours = readInt(props, "lookahead_hours", 48);

      var items = queryBus.ask(new ListCashierNextDrawsQuery(lookaheadHours, limit));
      return Map.of("items", items != null ? items : List.of());
    } catch (Exception e) {
      return Map.of("items", List.of());
    }
  }

  @Override
  public String providerKey() {
    return "cashier_next_draws";
  }

  private static int readInt(Map<String, Object> props, String key, int defaultValue) {
    if (props == null) return defaultValue;
    Object val = props.get(key);
    if (val instanceof Number n) return n.intValue();
    if (val instanceof String s) {
      try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {}
    }
    return defaultValue;
  }
}
