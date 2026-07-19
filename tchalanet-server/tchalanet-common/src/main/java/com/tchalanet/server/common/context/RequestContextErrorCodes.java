package com.tchalanet.server.common.context;

import com.tchalanet.server.common.web.error.ErrorAudience;
import com.tchalanet.server.common.web.error.ErrorCategory;
import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorRetryPolicy;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;

/** Stable client-facing failures owned by the authenticated request/access context. */
@UtilityClass
public class RequestContextErrorCodes {

  private static final Set<ErrorAudience> PRIVATE_AUDIENCES =
      Set.of(ErrorAudience.WEB_ADMIN, ErrorAudience.WEB_PLATFORM, ErrorAudience.MOBILE);

  public static final ErrorDescriptor TENANT_REQUIRED =
      new ErrorDescriptor(
          "access.context.tenant_required",
          ErrorCategory.BUSINESS_RULE,
          HttpStatus.UNPROCESSABLE_ENTITY,
          ErrorRetryPolicy.AFTER_USER_ACTION,
          PRIVATE_AUDIENCES,
          Set.of());

  public static final ErrorDescriptor TENANT_UNAVAILABLE =
      new ErrorDescriptor(
          "access.context.tenant_unavailable",
          ErrorCategory.AUTHORIZATION,
          HttpStatus.FORBIDDEN,
          ErrorRetryPolicy.AFTER_USER_ACTION,
          PRIVATE_AUDIENCES,
          Set.of());

  public static final ErrorDescriptor TENANT_OVERRIDE_REASON_REQUIRED =
      new ErrorDescriptor(
          "access.context.tenant_override_reason_required",
          ErrorCategory.AUTHORIZATION,
          HttpStatus.FORBIDDEN,
          ErrorRetryPolicy.AFTER_USER_ACTION,
          PRIVATE_AUDIENCES,
          Set.of());

  public static final ErrorDescriptor USER_NOT_BOOTSTRAPPED =
      new ErrorDescriptor(
          "access.context.user_not_bootstrapped",
          ErrorCategory.AUTHENTICATION,
          HttpStatus.UNAUTHORIZED,
          ErrorRetryPolicy.AFTER_REAUTH,
          PRIVATE_AUDIENCES,
          Set.of());

  public static final ErrorDescriptor SELLER_TERMINAL_REQUIRED =
      new ErrorDescriptor(
          "access.context.seller_terminal_required",
          ErrorCategory.BUSINESS_RULE,
          HttpStatus.UNPROCESSABLE_ENTITY,
          ErrorRetryPolicy.AFTER_USER_ACTION,
          PRIVATE_AUDIENCES,
          Set.of());

  public static final ErrorDescriptor OPERATIONAL_CONTEXT_REQUIRED =
      new ErrorDescriptor(
          "access.context.operational_context_required",
          ErrorCategory.BUSINESS_RULE,
          HttpStatus.UNPROCESSABLE_ENTITY,
          ErrorRetryPolicy.AFTER_USER_ACTION,
          PRIVATE_AUDIENCES,
          Set.of());

  public static final ErrorDescriptor OPERATIONAL_CONTEXT_UNTRUSTED =
      new ErrorDescriptor(
          "access.context.operational_context_untrusted",
          ErrorCategory.AUTHORIZATION,
          HttpStatus.FORBIDDEN,
          ErrorRetryPolicy.NEVER,
          PRIVATE_AUDIENCES,
          Set.of());

  public static Set<ErrorDescriptor> all() {
    return Set.of(
        TENANT_REQUIRED,
        TENANT_UNAVAILABLE,
        TENANT_OVERRIDE_REASON_REQUIRED,
        USER_NOT_BOOTSTRAPPED,
        SELLER_TERMINAL_REQUIRED,
        OPERATIONAL_CONTEXT_REQUIRED,
        OPERATIONAL_CONTEXT_UNTRUSTED);
  }
}
