package com.tchalanet.server.features.cashier.dashboard;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.core.pagemodel.api.dynamic.PageModelDynamicProvider;
import com.tchalanet.server.core.pagemodel.api.dynamic.PageModelDynamicProviderException;
import com.tchalanet.server.core.pagemodel.api.dynamic.PageModelResolutionContext;
import com.tchalanet.server.core.pagemodel.api.model.PageModelDoc;
import com.tchalanet.server.features.pagemodel.security.PageModelAllowedRoles;
import java.util.List;
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
 *   - dashboard.cashier.nextDraws
 *   - dashboard.cashier.recentTickets
 *   - dashboard.cashier.quickSale
 *   - dashboard.cashier.readiness  (operational context : ready, trusted, missing)
 *   - dashboard.cashier.alerts     (operational blockers + warnings)
 *   - dashboard.cashier.stats      (analytics KPIs for today, seller-scoped)
 *   - dashboard.cashier.offlineSync (offline/sync status placeholder)
 */
@Component
@RequiredArgsConstructor
@PageModelAllowedRoles(TchRole.CASHIER)
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
      case "dashboard.cashier.nextDraws" -> new ItemsPayload(payload.nextDraws());
      case "dashboard.cashier.recentTickets" -> new ItemsPayload(payload.recentTickets());
      case "dashboard.cashier.quickSale" -> QUICK_SALE;
      case "dashboard.cashier.readiness"    -> payload.readiness();
      case "dashboard.cashier.alerts"       -> payload.alerts();
      case "dashboard.cashier.stats"        -> payload.stats();
      case "dashboard.cashier.offlineSync" -> payload.offlineSync();
      default -> throw new PageModelDynamicProviderException(
          "CASHIER_DASHBOARD_UNKNOWN_WIDGET",
          "Unknown widgetId for source=" + SOURCE + ": " + widgetId);
    };
  }

  @Override
  public String providerKey() {
    return SOURCE;
  }

  /** Static payload for the quick-sale shortcut widget. */
  private static final QuickSalePayload QUICK_SALE =
      new QuickSalePayload("sellTicket", "cashier.quicksale.label", "/cashier/sell");

  /** Generic list wrapper — avoids raw Map for next_draws / recent_tickets dispatch. */
  record ItemsPayload(List<?> items) {}

  /** Quick-sale shortcut widget payload. */
  record QuickSalePayload(String actionId, String labelKey, String path) {}
}
