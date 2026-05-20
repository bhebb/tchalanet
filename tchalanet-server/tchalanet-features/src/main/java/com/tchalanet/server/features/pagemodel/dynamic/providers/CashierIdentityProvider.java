package com.tchalanet.server.features.pagemodel.dynamic.providers;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.core.pagemodel.internal.domain.model.PageModelDoc;
import com.tchalanet.server.core.session.api.query.GetCashierIdentityQuery;
import com.tchalanet.server.features.pagemodel.dynamic.PageModelDynamicProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CashierIdentityProvider implements PageModelDynamicProvider {

  private final QueryBus queryBus;

  @Override
  public boolean supports(String logicalId, String widgetType, String source) {
    return "cashier_identity".equals(source);
  }

  @Override
  public Object load(
      PageModelDoc pageModel,
      String widgetId,
      PageModelDoc.WidgetConfig widgetConfig,
      String lang,
      TchRequestContext ctx) {

    if (ctx == null || ctx.tenantId() == null || ctx.userId() == null) {
      return Map.of();
    }

    try {
      var view = queryBus.ask(new GetCashierIdentityQuery(ctx.tenantId(), ctx.userId()));
      var result = new HashMap<String, Object>();
      if (view.cashierDisplayName() != null) result.put("cashierDisplayName", view.cashierDisplayName());
      if (view.outletName() != null)         result.put("outletName", view.outletName());
      if (view.terminalLabel() != null)      result.put("terminalLabel", view.terminalLabel());
      if (view.tenantCode() != null)         result.put("tenantCode", view.tenantCode());
      return result;
    } catch (Exception e) {
      return Map.of();
    }
  }

  @Override
  public String providerKey() {
    return "cashier_identity";
  }
}
