package com.tchalanet.server.features.platformadmin.error;

import com.tchalanet.server.common.web.error.ErrorAudience;
import com.tchalanet.server.common.web.error.ErrorCategory;
import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorRetryPolicy;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;

/** Stable degradations owned by the commercial platform-admin dashboard. */
@UtilityClass
public class PlatformAdminErrorCodes {

  private static final Set<ErrorAudience> AUDIENCES = Set.of(ErrorAudience.WEB_PLATFORM);

  public static final ErrorDescriptor DASHBOARD_TENANTS_UNAVAILABLE =
      unavailable("platformadmin.dashboard.tenants_unavailable");
  public static final ErrorDescriptor DASHBOARD_ANALYTICS_UNAVAILABLE =
      unavailable("platformadmin.dashboard.analytics_unavailable");
  public static final ErrorDescriptor DASHBOARD_SUBSCRIPTIONS_UNAVAILABLE =
      unavailable("platformadmin.dashboard.subscriptions_unavailable");
  public static final ErrorDescriptor DASHBOARD_ONBOARDING_UNAVAILABLE =
      unavailable("platformadmin.dashboard.onboarding_unavailable");
  public static final ErrorDescriptor DASHBOARD_PUBLIC_CONTENT_UNAVAILABLE =
      unavailable("platformadmin.dashboard.public_content_unavailable");

  public static Set<ErrorDescriptor> all() {
    return Set.of(
        DASHBOARD_TENANTS_UNAVAILABLE,
        DASHBOARD_ANALYTICS_UNAVAILABLE,
        DASHBOARD_SUBSCRIPTIONS_UNAVAILABLE,
        DASHBOARD_ONBOARDING_UNAVAILABLE,
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
