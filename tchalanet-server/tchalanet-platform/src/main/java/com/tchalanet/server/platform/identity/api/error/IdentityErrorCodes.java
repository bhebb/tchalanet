package com.tchalanet.server.platform.identity.api.error;

import com.tchalanet.server.common.web.error.ErrorAudience;
import com.tchalanet.server.common.web.error.ErrorCategory;
import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorRetryPolicy;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;

/** Stable client-facing failures owned by platform identity. */
@UtilityClass
public class IdentityErrorCodes {

  private static final Set<ErrorAudience> LOGIN_AUDIENCES =
      Set.of(ErrorAudience.WEB_PUBLIC, ErrorAudience.WEB_ADMIN, ErrorAudience.WEB_PLATFORM);
  private static final Set<ErrorAudience> AUTHENTICATED_AUDIENCES =
      Set.of(ErrorAudience.WEB_ADMIN, ErrorAudience.WEB_PLATFORM, ErrorAudience.MOBILE);

  /**
   * Deliberately generic public-login failure.
   *
   * <p>Username lookup must not reveal whether a username exists, is active, or has a provider
   * identity. The client therefore receives one stable code for every rejected lookup.
   */
  public static final ErrorDescriptor AUTH_INVALID_CREDENTIALS =
      new ErrorDescriptor(
          "identity.auth.invalid_credentials",
          ErrorCategory.AUTHENTICATION,
          HttpStatus.FORBIDDEN,
          ErrorRetryPolicy.AFTER_USER_ACTION,
          LOGIN_AUDIENCES,
          Set.of());

  public static final ErrorDescriptor VERIFIED_IDENTITY_REQUIRED =
      authorization("identity.session.verified_identity_required");
  public static final ErrorDescriptor EXTERNAL_IDENTITY_NOT_LINKED =
      authorization("identity.session.external_identity_not_linked");
  public static final ErrorDescriptor USER_INACTIVE =
      authorization("identity.session.user_inactive");
  public static final ErrorDescriptor SELLER_TERMINAL_NOT_LINKED =
      authorization("identity.session.seller_terminal_not_linked");
  public static final ErrorDescriptor SELLER_TERMINAL_INACTIVE =
      authorization("identity.session.seller_terminal_inactive");
  public static final ErrorDescriptor HANDOFF_AUTHENTICATED_USER_REQUIRED =
      authentication("identity.handoff.authenticated_user_required", HttpStatus.UNAUTHORIZED);
  public static final ErrorDescriptor HANDOFF_TARGET_MISMATCH =
      authorization("identity.handoff.target_mismatch");
  public static final ErrorDescriptor HANDOFF_INVALID_CODE =
      authorization("identity.handoff.invalid_code");
  public static final ErrorDescriptor HANDOFF_TARGET_FORBIDDEN =
      authorization("identity.handoff.target_forbidden");
  public static final ErrorDescriptor HANDOFF_ENTRY_ROUTE_NOT_ALLOWED =
      validation("identity.handoff.entry_route_not_allowed", HttpStatus.BAD_REQUEST);
  public static final ErrorDescriptor HANDOFF_NOT_FOUND_OR_EXPIRED =
      notFound("identity.handoff.not_found_or_expired", HttpStatus.GONE);
  public static final ErrorDescriptor HANDOFF_REPLAYED =
      notFound("identity.handoff.replayed", HttpStatus.GONE);
  public static final ErrorDescriptor HANDOFF_EXPIRED =
      notFound("identity.handoff.expired", HttpStatus.GONE);
  public static final ErrorDescriptor HANDOFF_RATE_LIMITED =
      rateLimited("identity.handoff.rate_limited");
  public static final ErrorDescriptor HANDOFF_EXTERNAL_IDENTITY_MISSING =
      businessRule("identity.handoff.external_identity_missing", HttpStatus.UNPROCESSABLE_CONTENT);
  public static final ErrorDescriptor HANDOFF_CUSTOM_TOKEN_UNSUPPORTED =
      businessRule(
          "identity.handoff.custom_token_unsupported_provider", HttpStatus.UNPROCESSABLE_CONTENT);
  public static final ErrorDescriptor HANDOFF_CUSTOM_TOKEN_FAILED =
      unexpected("identity.handoff.custom_token_failed");
  public static final ErrorDescriptor USER_NOT_FOUND =
      notFound("identity.user.not_found", HttpStatus.NOT_FOUND);
  public static final ErrorDescriptor EXTERNAL_IDENTITY_NOT_LINKED_FOR_USER =
      businessRule("identity.external_identity.not_linked", HttpStatus.UNPROCESSABLE_CONTENT);
  public static final ErrorDescriptor MEMBERSHIP_CROSS_TENANT_FORBIDDEN =
      authorization("identity.membership.cross_tenant_forbidden");
  public static final ErrorDescriptor USER_ACTION_FORBIDDEN =
      authorization("identity.user.action_forbidden");
  public static final ErrorDescriptor USER_OUTSIDE_TENANT_SCOPE =
      authorization("identity.user.outside_tenant_scope");
  public static final ErrorDescriptor SUPER_ADMIN_ROLE_ASSIGNMENT_FORBIDDEN =
      authorization("identity.admin.super_admin_assignment_forbidden");
  public static final ErrorDescriptor PROFILE_LOCALE_INVALID =
      validation("identity.profile.locale_invalid", HttpStatus.BAD_REQUEST);
  public static final ErrorDescriptor PROFILE_TIME_ZONE_INVALID =
      validation("identity.profile.time_zone_invalid", HttpStatus.BAD_REQUEST);
  public static final ErrorDescriptor PROFILE_CURRENCY_INVALID =
      validation("identity.profile.currency_invalid", HttpStatus.BAD_REQUEST);
  public static final ErrorDescriptor ROLE_INVALID =
      validation("identity.role.invalid", HttpStatus.BAD_REQUEST);

  public static Set<ErrorDescriptor> all() {
    return Set.of(
        AUTH_INVALID_CREDENTIALS,
        VERIFIED_IDENTITY_REQUIRED,
        EXTERNAL_IDENTITY_NOT_LINKED,
        USER_INACTIVE,
        SELLER_TERMINAL_NOT_LINKED,
        SELLER_TERMINAL_INACTIVE,
        HANDOFF_AUTHENTICATED_USER_REQUIRED,
        HANDOFF_TARGET_MISMATCH,
        HANDOFF_INVALID_CODE,
        HANDOFF_TARGET_FORBIDDEN,
        HANDOFF_ENTRY_ROUTE_NOT_ALLOWED,
        HANDOFF_NOT_FOUND_OR_EXPIRED,
        HANDOFF_REPLAYED,
        HANDOFF_EXPIRED,
        HANDOFF_RATE_LIMITED,
        HANDOFF_EXTERNAL_IDENTITY_MISSING,
        HANDOFF_CUSTOM_TOKEN_UNSUPPORTED,
        HANDOFF_CUSTOM_TOKEN_FAILED,
        USER_NOT_FOUND,
        EXTERNAL_IDENTITY_NOT_LINKED_FOR_USER,
        MEMBERSHIP_CROSS_TENANT_FORBIDDEN,
        USER_ACTION_FORBIDDEN,
        USER_OUTSIDE_TENANT_SCOPE,
        SUPER_ADMIN_ROLE_ASSIGNMENT_FORBIDDEN,
        PROFILE_LOCALE_INVALID,
        PROFILE_TIME_ZONE_INVALID,
        PROFILE_CURRENCY_INVALID,
        ROLE_INVALID);
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

  private static ErrorDescriptor authentication(String code, HttpStatus status) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.AUTHENTICATION,
        status,
        ErrorRetryPolicy.AFTER_REAUTH,
        AUTHENTICATED_AUDIENCES,
        Set.of());
  }

  private static ErrorDescriptor validation(String code, HttpStatus status) {
    return descriptor(code, ErrorCategory.VALIDATION, status, ErrorRetryPolicy.AFTER_USER_ACTION);
  }

  private static ErrorDescriptor notFound(String code, HttpStatus status) {
    return descriptor(code, ErrorCategory.NOT_FOUND, status, ErrorRetryPolicy.AFTER_USER_ACTION);
  }

  private static ErrorDescriptor rateLimited(String code) {
    return descriptor(
        code,
        ErrorCategory.RATE_LIMITED,
        HttpStatus.TOO_MANY_REQUESTS,
        ErrorRetryPolicy.AFTER_DELAY);
  }

  private static ErrorDescriptor businessRule(String code, HttpStatus status) {
    return descriptor(
        code, ErrorCategory.BUSINESS_RULE, status, ErrorRetryPolicy.AFTER_USER_ACTION);
  }

  private static ErrorDescriptor unexpected(String code) {
    return descriptor(
        code,
        ErrorCategory.UNEXPECTED,
        HttpStatus.INTERNAL_SERVER_ERROR,
        ErrorRetryPolicy.AFTER_DELAY);
  }

  private static ErrorDescriptor descriptor(
      String code, ErrorCategory category, HttpStatus status, ErrorRetryPolicy retryPolicy) {
    return new ErrorDescriptor(
        code, category, status, retryPolicy, AUTHENTICATED_AUDIENCES, Set.of());
  }
}
