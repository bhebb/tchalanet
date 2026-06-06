package com.tchalanet.server.features.platformadmin.dashboard;

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
 * Single provider for source {@code platform_admin_dashboard} (dashboard-overview-runtime-v1).
 * Loads the bundled payload once per request via {@link PageModelResolutionContext}
 * and dispatches the relevant slice by widgetId.
 *
 * Supported widget ids:
 *   - dashboard.superadmin.health
 *   - dashboard.superadmin.tenants
 *   - dashboard.superadmin.subscriptions
 *   - dashboard.superadmin.onboarding
 *   - dashboard.superadmin.alerts
 *   - dashboard.superadmin.publicContent
 *   - dashboard.superadmin.quickActions
 */
@Component
@RequiredArgsConstructor
@PageModelAllowedRoles(TchRole.SUPER_ADMIN)
public class PlatformAdminDashboardProvider implements PageModelDynamicProvider {

  static final String SOURCE = "platform_admin_dashboard";
  private static final String MEMO_KEY = SOURCE;

  private final PlatformAdminDashboardPayloadAssembler assembler;

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

    PlatformAdminDashboardPayloadAssembler.Payload payload =
        resolutionContext.getOrLoad(MEMO_KEY, () -> assembler.assemble(ctx));

    return switch (widgetId == null ? "" : widgetId) {
      case "dashboard.superadmin.health" -> payload.health();
      case "dashboard.superadmin.tenants" -> payload.tenants();
      case "dashboard.superadmin.subscriptions" -> payload.subscriptions();
      case "dashboard.superadmin.onboarding" -> payload.onboardingAlerts();
      case "dashboard.superadmin.alerts" -> payload.platformAlerts();
      case "dashboard.superadmin.publicContent" -> payload.publicContent();
      case "dashboard.superadmin.quickActions" -> payload.quickActions();
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
