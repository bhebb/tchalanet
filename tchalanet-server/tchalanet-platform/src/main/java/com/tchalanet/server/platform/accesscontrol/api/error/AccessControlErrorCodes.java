package com.tchalanet.server.platform.accesscontrol.api.error;

import com.tchalanet.server.common.web.error.ErrorAudience;
import com.tchalanet.server.common.web.error.ErrorCategory;
import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorRetryPolicy;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;

/** Stable client-facing authorization failures owned by platform access control. */
@UtilityClass
public class AccessControlErrorCodes {

  private static final Set<ErrorAudience> AUTHENTICATED_AUDIENCES =
      Set.of(ErrorAudience.WEB_ADMIN, ErrorAudience.WEB_PLATFORM, ErrorAudience.MOBILE);

  public static final ErrorDescriptor ACTOR_TYPE_UNSUPPORTED =
      authorization("access.actor.type_unsupported");
  public static final ErrorDescriptor TERMINAL_TENANT_SELECTION_NOT_ALLOWED =
      authorization("access.terminal.tenant_selection_not_allowed");
  public static final ErrorDescriptor TENANT_OVERRIDE_NOT_SUPER_ADMIN =
      authorization("access.tenant.override_not_super_admin");
  public static final ErrorDescriptor TENANT_OVERRIDE_FORBIDDEN =
      authorization("access.tenant.override_forbidden");
  public static final ErrorDescriptor TENANT_OVERRIDE_REASON_REQUIRED =
      validation("access.tenant.override_reason_required");
  public static final ErrorDescriptor TENANT_OVERRIDE_INVALID =
      validation("access.tenant.override_invalid");
  public static final ErrorDescriptor TENANT_AMBIGUOUS_MEMBERSHIP =
      authorization("access.tenant.ambiguous_membership");
  public static final ErrorDescriptor SUPPORT_AUTHENTICATED_ACTOR_REQUIRED =
      authentication("access.support.authenticated_actor_required");
  public static final ErrorDescriptor SUPPORT_TENANT_REQUIRED =
      validation("access.support.tenant_required");
  public static final ErrorDescriptor SUPPORT_REASON_REQUIRED =
      validation("access.support.reason_required");
  public static final ErrorDescriptor ROLE_NOT_FOUND = notFound("access.role.not_found");
  public static final ErrorDescriptor PERMISSION_NOT_FOUND =
      notFound("access.permission.not_found");
  public static final ErrorDescriptor ROLE_PERMISSION_INPUT_REQUIRED =
      validation("access.role_permission_input_required");
  public static final ErrorDescriptor PLATFORM_ROLE_UNAVAILABLE =
      serviceUnavailable("access.platform_role_unavailable");

  public static Set<ErrorDescriptor> all() {
    return Set.of(
        ACTOR_TYPE_UNSUPPORTED,
        TERMINAL_TENANT_SELECTION_NOT_ALLOWED,
        TENANT_OVERRIDE_NOT_SUPER_ADMIN,
        TENANT_OVERRIDE_FORBIDDEN,
        TENANT_OVERRIDE_REASON_REQUIRED,
        TENANT_OVERRIDE_INVALID,
        TENANT_AMBIGUOUS_MEMBERSHIP,
        SUPPORT_AUTHENTICATED_ACTOR_REQUIRED,
        SUPPORT_TENANT_REQUIRED,
        SUPPORT_REASON_REQUIRED,
        ROLE_NOT_FOUND,
        PERMISSION_NOT_FOUND,
        ROLE_PERMISSION_INPUT_REQUIRED,
        PLATFORM_ROLE_UNAVAILABLE);
  }

  private static ErrorDescriptor authorization(String code) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.AUTHORIZATION,
        HttpStatus.FORBIDDEN,
        ErrorRetryPolicy.AFTER_REAUTH,
        AUTHENTICATED_AUDIENCES,
        Set.of());
  }

  private static ErrorDescriptor authentication(String code) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.AUTHENTICATION,
        HttpStatus.UNAUTHORIZED,
        ErrorRetryPolicy.AFTER_REAUTH,
        AUTHENTICATED_AUDIENCES,
        Set.of());
  }

  private static ErrorDescriptor validation(String code) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.VALIDATION,
        HttpStatus.BAD_REQUEST,
        ErrorRetryPolicy.AFTER_USER_ACTION,
        AUTHENTICATED_AUDIENCES,
        Set.of());
  }

  private static ErrorDescriptor notFound(String code) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.NOT_FOUND,
        HttpStatus.NOT_FOUND,
        ErrorRetryPolicy.NEVER,
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
