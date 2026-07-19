package com.tchalanet.server.platform.tenant.api.error;

import com.tchalanet.server.common.web.error.ErrorAudience;
import com.tchalanet.server.common.web.error.ErrorCategory;
import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorRetryPolicy;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;

/** Stable client-facing failures for tenant lifecycle and settings operations. */
@UtilityClass
public class TenantErrorCodes {

  private static final Set<ErrorAudience> AUDIENCES =
      Set.of(ErrorAudience.WEB_ADMIN, ErrorAudience.WEB_PLATFORM, ErrorAudience.MOBILE);

  public static final ErrorDescriptor NOT_FOUND = notFound("tenant.not_found");
  public static final ErrorDescriptor ACTIVATION_NOT_ALLOWED =
      businessRule("tenant.activation_not_allowed");
  public static final ErrorDescriptor SUSPENSION_NOT_ALLOWED =
      businessRule("tenant.suspension_not_allowed");
  public static final ErrorDescriptor SETTINGS_INVALID = validation("tenant.settings_invalid");
  public static final ErrorDescriptor SUPPORT_REASON_INVALID =
      validation("tenant.support_reason_invalid");
  public static final ErrorDescriptor COMMISSION_DEFAULT_RATE_NOT_SET =
      businessRule("tenant.commission.default_rate_not_set");

  public static Set<ErrorDescriptor> all() {
    return Set.of(
        NOT_FOUND,
        ACTIVATION_NOT_ALLOWED,
        SUSPENSION_NOT_ALLOWED,
        SETTINGS_INVALID,
        SUPPORT_REASON_INVALID,
        COMMISSION_DEFAULT_RATE_NOT_SET);
  }

  private static ErrorDescriptor notFound(String code) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.NOT_FOUND,
        HttpStatus.NOT_FOUND,
        ErrorRetryPolicy.NEVER,
        AUDIENCES,
        Set.of());
  }

  private static ErrorDescriptor businessRule(String code) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.BUSINESS_RULE,
        HttpStatus.UNPROCESSABLE_ENTITY,
        ErrorRetryPolicy.AFTER_USER_ACTION,
        AUDIENCES,
        Set.of());
  }

  private static ErrorDescriptor validation(String code) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.VALIDATION,
        HttpStatus.UNPROCESSABLE_ENTITY,
        ErrorRetryPolicy.AFTER_USER_ACTION,
        AUDIENCES,
        Set.of());
  }
}
