package com.tchalanet.server.features.pagemodel.dynamic.providers;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.core.pagemodel.domain.model.PageModelDoc;
import com.tchalanet.server.core.session.application.query.model.ListCashierOpenSessionsQuery;
import com.tchalanet.server.features.pagemodel.dynamic.PageModelDynamicProvider;
import java.math.BigDecimal;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * E.4 — Provider Vue d'ensemble caissier.
 * Source : "cashier_overview"
 * Branche sur ListCashierOpenSessionsQuery pour données réelles.
 */
@Component
@RequiredArgsConstructor
public class CashierOverviewProvider implements PageModelDynamicProvider {

  private final QueryBus queryBus;

  @Override
  public boolean supports(String logicalId, String widgetType, String source) {
    return "private.dashboard.cashier".equals(logicalId) && "cashier_overview".equals(source);
  }

  @Override
  public Object load(
      PageModelDoc pageModel,
      String widgetId,
      PageModelDoc.WidgetConfig widgetConfig,
      String lang,
      TchRequestContext ctx) {

    // Fallback si contexte manquant
    if (ctx == null || ctx.tenantId() == null || ctx.userId() == null) {
      return fallback();
    }

    try {
      var sessions =
          queryBus.ask(new ListCashierOpenSessionsQuery(ctx.tenantId(), ctx.userId()));

      if (sessions == null || sessions.isEmpty()) {
        return Map.of(
            "ticketsToday", 0L,
            "totalAmount", BigDecimal.ZERO,
            "sessionOpen", false);
      }

      // Première session ouverte
      var session = sessions.get(0);
      return Map.of(
          "ticketsToday", session.ticketsSold(),
          "totalAmount", session.totalSales() != null ? session.totalSales() : BigDecimal.ZERO,
          "sessionOpen", true,
          "sessionId", session.sessionId() != null ? session.sessionId().toString() : "",
          "openedAt", session.openedAt() != null ? session.openedAt().toString() : "");
    } catch (Exception e) {
      return fallback();
    }
  }

  private Map<String, Object> fallback() {
    return Map.of(
        "ticketsToday", 0L,
        "totalAmount", BigDecimal.ZERO,
        "sessionOpen", false);
  }

  @Override
  public String providerKey() {
    return "cashier_overview";
  }
}
