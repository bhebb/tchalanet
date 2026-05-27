package com.tchalanet.server.features.tenantadmin.dashboard;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.core.pagemodel.api.dynamic.PageModelDynamicProvider;
import com.tchalanet.server.core.pagemodel.api.dynamic.PageModelDynamicProviderException;
import com.tchalanet.server.core.pagemodel.api.dynamic.PageModelResolutionContext;
import com.tchalanet.server.core.pagemodel.api.model.PageModelDoc;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Single provider for source {@code tenant_admin_dashboard} (dashboard-overview-runtime-v1).
 * Loads the bundled payload once per request via {@link PageModelResolutionContext}
 * and dispatches the relevant slice by widgetId.
 *
 * Supported widget ids:
 *   - dashboard.tenant_admin.header
 *   - dashboard.tenant_admin.kpis
 *   - dashboard.tenant_admin.readiness
 *   - dashboard.tenant_admin.alerts
 *   - dashboard.tenant_admin.operations
 *   - dashboard.tenant_admin.commercial
 *   - dashboard.tenant_admin.quick_actions
 */
@Component
@RequiredArgsConstructor
public class TenantAdminDashboardProvider implements PageModelDynamicProvider {

  static final String SOURCE = "tenant_admin_dashboard";
  private static final String MEMO_KEY = SOURCE;

  private final TenantAdminDashboardPayloadAssembler assembler;

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

    TenantAdminDashboardPayloadAssembler.Payload payload =
        resolutionContext.getOrLoad(MEMO_KEY, () -> assembler.assemble(ctx));

    return switch (widgetId == null ? "" : widgetId) {
      case "dashboard.tenant_admin.header" -> payload.header();
      case "dashboard.tenant_admin.kpis" -> payload.kpis();
      case "dashboard.tenant_admin.readiness" -> payload.readiness();
      case "dashboard.tenant_admin.alerts" -> payload.alerts();
      case "dashboard.tenant_admin.operations" -> payload.operations();
      case "dashboard.tenant_admin.commercial" -> payload.commercial();
      case "dashboard.tenant_admin.quick_actions" -> payload.quickActions();
      default -> throw new PageModelDynamicProviderException(
          "TENANT_ADMIN_DASHBOARD_UNKNOWN_WIDGET",
          "Unknown widgetId for source=" + SOURCE + ": " + widgetId);
    };
  }

  @Override
  public String providerKey() {
    return SOURCE;
  }
}
