package com.tchalanet.server.platform.entitlement.api.error;

import com.tchalanet.server.common.web.error.ErrorAudience;
import com.tchalanet.server.common.web.error.ErrorCategory;
import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorRetryPolicy;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;

/** Stable client-facing capability failures owned by platform entitlement. */
@UtilityClass
public class EntitlementErrorCodes {

  private static final Set<ErrorAudience> AUTHENTICATED_AUDIENCES =
      Set.of(ErrorAudience.WEB_ADMIN, ErrorAudience.WEB_PLATFORM, ErrorAudience.MOBILE);

  public static final ErrorDescriptor FEATURE_REQUIRED =
      businessRule("entitlement.feature_required");
  public static final ErrorDescriptor LIMIT_MISSING =
      serviceUnavailable("entitlement.limit_missing");
  public static final ErrorDescriptor LIMIT_EXCEEDED = businessRule("entitlement.limit_exceeded");
  public static final ErrorDescriptor USAGE_PROVIDER_UNAVAILABLE =
      serviceUnavailable("entitlement.usage_provider_unavailable");
  public static final ErrorDescriptor PLAN_UNAVAILABLE =
      serviceUnavailable("entitlement.plan_unavailable");

  public static Set<ErrorDescriptor> all() {
    return Set.of(
        FEATURE_REQUIRED,
        LIMIT_MISSING,
        LIMIT_EXCEEDED,
        USAGE_PROVIDER_UNAVAILABLE,
        PLAN_UNAVAILABLE);
  }

  private static ErrorDescriptor businessRule(String code) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.BUSINESS_RULE,
        HttpStatus.FORBIDDEN,
        ErrorRetryPolicy.AFTER_USER_ACTION,
        AUTHENTICATED_AUDIENCES,
        Set.of());
  }

  private static ErrorDescriptor serviceUnavailable(String code) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.SERVICE_UNAVAILABLE,
        HttpStatus.SERVICE_UNAVAILABLE,
        ErrorRetryPolicy.AFTER_DELAY,
        AUTHENTICATED_AUDIENCES,
        Set.of());
  }
}
