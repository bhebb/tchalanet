package com.tchalanet.server.features.cashier.dashboard;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.core.pagemodel.api.dynamic.PageModelDynamicProvider;
import com.tchalanet.server.core.pagemodel.api.dynamic.PageModelDynamicProviderException;
import com.tchalanet.server.core.pagemodel.api.dynamic.PageModelResolutionContext;
import com.tchalanet.server.core.pagemodel.api.model.PageModelDoc;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Single provider for source {@code cashier_dashboard} (dashboard-overview-runtime-v1).
 * Loads the bundled payload once per request via {@link PageModelResolutionContext}
 * and dispatches the relevant slice by widgetId.
 *
 * Supported widget ids (cashier WEB only — POS/mobile uses /tenant/cashier/home):
 *   - dashboard.cashier.identity
 *   - dashboard.cashier.session
 *   - dashboard.cashier.overview
 *   - dashboard.cashier.next_draws
 *   - dashboard.cashier.recent_tickets
 *   - dashboard.cashier.quick_sale
 *   - dashboard.cashier.readiness  (operational context : ready, trusted, missing)
 *   - dashboard.cashier.alerts     (operational blockers + warnings)
 */
@Component
@RequiredArgsConstructor
public class CashierWebDashboardProvider implements PageModelDynamicProvider {

  static final String SOURCE = "cashier_dashboard";
  private static final String MEMO_KEY = SOURCE;

  private final CashierDashboardPayloadAssembler assembler;

  @Override
  public boolean supports(String logicalId, String widgetType, String source) {
    return SOURCE.equals(source);
  }

  @Override
  public Object load(
      PageModelDoc pageModel,
      String widgetId,
      PageModelDoc.WidgetConfig widgetConfig,
      String lang,
      TchRequestContext ctx,
      PageModelResolutionContext resolutionContext) {

    CashierDashboardPayloadAssembler.Payload payload =
        resolutionContext.getOrLoad(MEMO_KEY, () -> assembler.assemble(ctx));

    return switch (widgetId == null ? "" : widgetId) {
      case "dashboard.cashier.identity" -> payload.identity();
      case "dashboard.cashier.session" -> payload.session();
      case "dashboard.cashier.overview" -> payload.overview();
      case "dashboard.cashier.next_draws" -> Map.of("items", payload.nextDraws());
      case "dashboard.cashier.recent_tickets" -> Map.of("items", payload.recentTickets());
      case "dashboard.cashier.quick_sale" -> Map.of(
          "actionId", "SELL_TICKET",
          "label", "Vendre un ticket",
          "route", "/cashier/sell");
      case "dashboard.cashier.readiness" -> payload.readiness();
      case "dashboard.cashier.alerts" -> payload.alerts();
      default -> throw new PageModelDynamicProviderException(
          "CASHIER_DASHBOARD_UNKNOWN_WIDGET",
          "Unknown widgetId for source=" + SOURCE + ": " + widgetId);
    };
  }

  @Override
  public String providerKey() {
    return SOURCE;
  }
}
