package com.tchalanet.server.features.pagemodel.dynamic.providers;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.core.pagemodel.api.model.PageModelDoc;
import com.tchalanet.server.core.session.api.query.GetCashierSessionSummaryQuery;
import com.tchalanet.server.core.pagemodel.api.dynamic.PageModelDynamicProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class CashierSessionProvider implements PageModelDynamicProvider {

  private final QueryBus queryBus;

  @Override
  public boolean supports(String logicalId, String widgetType, String source) {
    return ("private.dashboard.cashier".equals(logicalId) || "private.dashboard.cashier.web".equals(logicalId))
        && "cashier_session".equals(source);
  }

  @Override
  public Object load(
      PageModelDoc pageModel,
      String widgetId,
      PageModelDoc.WidgetConfig widgetConfig,
      String lang,
      TchRequestContext ctx,
      com.tchalanet.server.core.pagemodel.api.dynamic.PageModelResolutionContext resolutionContext) {

    if (ctx == null || ctx.tenantId() == null || ctx.userId() == null) {
      return Map.of("active", false);
    }

    try {
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
    } catch (Exception e) {
      return Map.of("active", false);
    }
  }

  @Override
  public String providerKey() {
    return "cashier_session";
  }
}
