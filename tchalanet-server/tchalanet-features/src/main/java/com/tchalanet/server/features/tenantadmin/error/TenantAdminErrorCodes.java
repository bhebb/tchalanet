package com.tchalanet.server.features.tenantadmin.error;

import com.tchalanet.server.common.web.error.ErrorAudience;
import com.tchalanet.server.common.web.error.ErrorCategory;
import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorRetryPolicy;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;

/** Stable client-facing failures and degradations owned by tenant-admin feature surfaces. */
@UtilityClass
public class TenantAdminErrorCodes {

  private static final Set<ErrorAudience> AUDIENCES = Set.of(ErrorAudience.WEB_ADMIN);

  public static final ErrorDescriptor OVERVIEW_ADDRESS_UNAVAILABLE =
      unavailable("tenantadmin.overview.address_unavailable");
  public static final ErrorDescriptor OVERVIEW_REGISTRY_UNAVAILABLE =
      unavailable("tenantadmin.overview.registry_unavailable");
  public static final ErrorDescriptor DASHBOARD_REGISTRY_UNAVAILABLE =
      unavailable("tenantadmin.dashboard.registry_unavailable");
  public static final ErrorDescriptor DASHBOARD_OPERATIONS_UNAVAILABLE =
      unavailable("tenantadmin.dashboard.operations_unavailable");
  public static final ErrorDescriptor DASHBOARD_COMMERCIAL_UNAVAILABLE =
      unavailable("tenantadmin.dashboard.commercial_unavailable");
  public static final ErrorDescriptor DASHBOARD_KPIS_UNAVAILABLE =
      unavailable("tenantadmin.dashboard.kpis_unavailable");
  public static final ErrorDescriptor DASHBOARD_ANALYTICS_UNAVAILABLE =
      unavailable("tenantadmin.dashboard.analytics_unavailable");
  public static final ErrorDescriptor DASHBOARD_OPEN_DRAWS_UNAVAILABLE =
      unavailable("tenantadmin.dashboard.open_draws_unavailable");
  public static final ErrorDescriptor DASHBOARD_CLOSED_DRAWS_UNAVAILABLE =
      unavailable("tenantadmin.dashboard.closed_draws_unavailable");
  public static final ErrorDescriptor DASHBOARD_NOTIFICATIONS_UNAVAILABLE =
      unavailable("tenantadmin.dashboard.notifications_unavailable");
  public static final ErrorDescriptor DASHBOARD_COMMISSION_UNAVAILABLE =
      unavailable("tenantadmin.dashboard.commission_unavailable");
  public static final ErrorDescriptor DASHBOARD_PUBLIC_CONTENT_UNAVAILABLE =
      unavailable("tenantadmin.dashboard.public_content_unavailable");

  public static Set<ErrorDescriptor> all() {
    return Set.of(
        OVERVIEW_ADDRESS_UNAVAILABLE,
        OVERVIEW_REGISTRY_UNAVAILABLE,
        DASHBOARD_REGISTRY_UNAVAILABLE,
        DASHBOARD_OPERATIONS_UNAVAILABLE,
        DASHBOARD_COMMERCIAL_UNAVAILABLE,
        DASHBOARD_KPIS_UNAVAILABLE,
        DASHBOARD_ANALYTICS_UNAVAILABLE,
        DASHBOARD_OPEN_DRAWS_UNAVAILABLE,
        DASHBOARD_CLOSED_DRAWS_UNAVAILABLE,
        DASHBOARD_NOTIFICATIONS_UNAVAILABLE,
        DASHBOARD_COMMISSION_UNAVAILABLE,
        DASHBOARD_PUBLIC_CONTENT_UNAVAILABLE);
  }

  private static ErrorDescriptor unavailable(String code) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.SERVICE_UNAVAILABLE,
        HttpStatus.SERVICE_UNAVAILABLE,
        ErrorRetryPolicy.RETRY_SAME_INTENT,
        AUDIENCES,
        Set.of());
  }
}
