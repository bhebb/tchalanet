package com.tchalanet.server.features.tenantadmin.dashboard;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.core.pagemodel.api.dynamic.PageModelDynamicProvider;
import com.tchalanet.server.core.pagemodel.api.dynamic.PageModelDynamicProviderException;
import com.tchalanet.server.core.pagemodel.api.dynamic.PageModelResolutionContext;
import com.tchalanet.server.core.pagemodel.api.model.PageModelDoc;
import com.tchalanet.server.features.pagemodel.security.PageModelAllowedRoles;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Single provider for source {@code tenant_admin_dashboard} (dashboard-overview-runtime-v1).
 * Loads the bundled payload once per request via {@link PageModelResolutionContext}
 * and dispatches the relevant slice by widgetId.
 *
 * Supported widget ids:
 *   - dashboard.tenantAdmin.header
 *   - dashboard.tenantAdmin.kpis
 *   - dashboard.tenantAdmin.readiness
 *   - dashboard.tenantAdmin.alerts
 *   - dashboard.tenantAdmin.operations
 *   - dashboard.tenantAdmin.commercial
 *   - dashboard.tenantAdmin.commission
 *   - dashboard.tenantAdmin.publicContent
 *   - dashboard.tenantAdmin.quickActions
 */
@Component
@RequiredArgsConstructor
@PageModelAllowedRoles(TchRole.TENANT_ADMIN)
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
      case "dashboard.tenantAdmin.header" -> payload.header();
      case "dashboard.tenantAdmin.kpis" -> payload.kpis();
      case "dashboard.tenantAdmin.readiness" -> payload.readiness();
      case "dashboard.tenantAdmin.alerts" -> payload.alerts();
      case "dashboard.tenantAdmin.operations" -> payload.operations();
      case "dashboard.tenantAdmin.commercial" -> payload.commercial();
      case "dashboard.tenantAdmin.commission" -> payload.commission();
      case "dashboard.tenantAdmin.publicContent" -> payload.publicContent();
      case "dashboard.tenantAdmin.quickActions" -> payload.quickActions();
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
