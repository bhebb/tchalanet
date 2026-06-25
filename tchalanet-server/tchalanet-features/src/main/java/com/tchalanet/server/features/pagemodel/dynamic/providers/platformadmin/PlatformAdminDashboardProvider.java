package com.tchalanet.server.features.pagemodel.dynamic.providers.platformadmin;

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
 * Single provider for source {@code platform_admin_dashboard}.
 * The page {@code logicalId} selects the dashboard payload assembler through
 * {@link PlatformAdminDashboardPayloadService}; the provider only dispatches the
 * resolved payload slice by widgetId.
 *
 * Supported commercial widget ids:
 *   - dashboard.superadmin.tenants
 *   - dashboard.superadmin.platformSales
 *   - dashboard.superadmin.salesTrend
 *   - dashboard.superadmin.gameBreakdown
 *   - dashboard.superadmin.subscriptions
 *   - dashboard.superadmin.onboarding
 *   - dashboard.superadmin.publicContent
 *   - dashboard.superadmin.topTenants
 *   - dashboard.superadmin.quickActions
 *
 * Supported ops widget ids:
 *   - dashboard.superadmin.ops.overview
 *   - dashboard.superadmin.ops.health
 *   - dashboard.superadmin.ops.scheduler
 *   - dashboard.superadmin.ops.schedulerIssues
 *   - dashboard.superadmin.ops.resourcesCritical
 *   - dashboard.superadmin.ops.resources
 *   - dashboard.superadmin.ops.databaseCapacity
 *   - dashboard.superadmin.ops.notifications
 *   - dashboard.superadmin.ops.contactRequests
 *   - dashboard.superadmin.ops.quickActions
 */
@Component
@RequiredArgsConstructor
@PageModelAllowedRoles(TchRole.SUPER_ADMIN)
public class PlatformAdminDashboardProvider implements PageModelDynamicProvider {

  static final String SOURCE = "platform_admin_dashboard";

  private final PlatformAdminDashboardPayloadService payloadService;

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

    String logicalId = pageModel != null && pageModel.meta() != null ? pageModel.meta().id() : null;
    String memoKey = SOURCE + ":" + (logicalId == null ? "" : logicalId);
    Object payload =
        resolutionContext.getOrLoad(memoKey, () -> payloadService.assemble(logicalId, ctx));

    if (payload instanceof PlatformAdminOpsDashboardPayloadAssembler.Payload opsPayload) {
      return switch (widgetId == null ? "" : widgetId) {
        case "dashboard.superadmin.ops.overview" -> opsPayload;
        case "dashboard.superadmin.ops.health" -> opsPayload.health();
        case "dashboard.superadmin.ops.scheduler",
            "dashboard.superadmin.ops.schedulerIssues" -> opsPayload.schedulerSummary();
        case "dashboard.superadmin.ops.resourcesCritical",
            "dashboard.superadmin.ops.resources",
            "dashboard.superadmin.ops.databaseCapacity" -> opsPayload.resourceSummary();
        case "dashboard.superadmin.ops.notifications" -> opsPayload.appNotifications();
        case "dashboard.superadmin.ops.contactRequests" -> opsPayload.contactRequests();
        case "dashboard.superadmin.ops.quickActions" -> opsPayload.quickActions();
        default -> throw new PageModelDynamicProviderException(
            "PLATFORM_ADMIN_DASHBOARD_UNKNOWN_WIDGET",
            "Unknown ops widgetId for source=" + SOURCE + ": " + widgetId);
      };
    }

    if (!(payload instanceof PlatformAdminDashboardPayloadAssembler.Payload commercialPayload)) {
      throw new PageModelDynamicProviderException(
          "PLATFORM_ADMIN_DASHBOARD_UNSUPPORTED_PAYLOAD",
          "Unsupported payload for source=" + SOURCE + ", logicalId=" + logicalId);
    }

    return switch (widgetId == null ? "" : widgetId) {
      case "dashboard.superadmin.tenants" -> commercialPayload.tenants();
      case "dashboard.superadmin.platformSales" -> commercialPayload.sales();
      case "dashboard.superadmin.salesTrend" -> commercialPayload.sales();
      case "dashboard.superadmin.gameBreakdown" -> commercialPayload.sales();
      case "dashboard.superadmin.subscriptions" -> commercialPayload.subscriptions();
      case "dashboard.superadmin.onboarding" -> commercialPayload.onboardingAlerts();
      case "dashboard.superadmin.publicContent" -> commercialPayload.publicContent();
      case "dashboard.superadmin.topTenants" -> commercialPayload.tenantRanking();
      case "dashboard.superadmin.quickActions" -> commercialPayload.quickActions();
      default -> throw new PageModelDynamicProviderException(
          "PLATFORM_ADMIN_DASHBOARD_UNKNOWN_WIDGET",
          "Unknown widgetId for source=" + SOURCE + ": " + widgetId);
    };
  }

  @Override
  public String providerKey() {
    return SOURCE;
  }
}
