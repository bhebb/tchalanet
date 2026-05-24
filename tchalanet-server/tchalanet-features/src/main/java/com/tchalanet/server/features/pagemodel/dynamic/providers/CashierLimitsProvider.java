package com.tchalanet.server.features.pagemodel.dynamic.providers;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.core.limitpolicy.api.query.GetLimitsOverviewQuery;
import com.tchalanet.server.core.limitpolicy.api.model.LimitScopeRef;
import com.tchalanet.server.core.pagemodel.internal.domain.model.PageModelDoc;
import com.tchalanet.server.features.pagemodel.dynamic.PageModelDynamicProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CashierLimitsProvider implements PageModelDynamicProvider {

  private final QueryBus queryBus;

  @Override
  public boolean supports(String logicalId, String widgetType, String source) {
    return "private.dashboard.cashier".equals(logicalId) && "cashier_limits".equals(source);
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
      var view = queryBus.ask(new GetLimitsOverviewQuery(LimitScopeRef.agent(ctx.userId())));
      if (view == null || view.assignments() == null) {
        return Map.of("items", List.of());
      }
      var items = view.assignments().stream()
          .filter(a -> a.enabled())
          .map(a -> Map.of(
              "ruleKey", a.ruleKey().name(),
              "onBreach", a.onBreach().name(),
              "params", a.params()))
          .toList();
      return Map.of("items", items);
    } catch (Exception e) {
      return Map.of("items", List.of());
    }
  }

  @Override
  public String providerKey() {
    return "cashier_limits";
  }
}
