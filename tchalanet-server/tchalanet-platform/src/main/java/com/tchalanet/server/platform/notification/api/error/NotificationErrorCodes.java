package com.tchalanet.server.platform.notification.api.error;

import com.tchalanet.server.common.web.error.ErrorAudience;
import com.tchalanet.server.common.web.error.ErrorCategory;
import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorRetryPolicy;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;

/** Stable client-facing failures owned by platform notification. */
@UtilityClass
public class NotificationErrorCodes {

  private static final Set<ErrorAudience> AUTHENTICATED_AUDIENCES =
      Set.of(ErrorAudience.WEB_ADMIN, ErrorAudience.WEB_PLATFORM, ErrorAudience.MOBILE);

  public static final ErrorDescriptor NOT_FOUND = notFound("notification.not_found");
  public static final ErrorDescriptor PUBLICATION_NOT_FOUND =
      notFound("notification.publication_not_found");
  public static final ErrorDescriptor NOT_PUBLISHABLE =
      businessRule("notification.not_publishable");
  public static final ErrorDescriptor NOT_REPUBLISHABLE =
      businessRule("notification.not_republishable");
  public static final ErrorDescriptor REASON_REQUIRED = validation("notification.reason_required");
  public static final ErrorDescriptor SYSTEM_KEYS_REQUIRED =
      validation("notification.system_keys_required");
  public static final ErrorDescriptor TRANSLATION_REQUIRED =
      validation("notification.translation_required");
  public static final ErrorDescriptor TRANSLATION_LOCALE_UNSUPPORTED =
      validation("notification.translation_locale_unsupported");
  public static final ErrorDescriptor TENANT_REQUIRED = validation("notification.tenant_required");
  public static final ErrorDescriptor TARGET_REQUIRED = validation("notification.target_required");
  public static final ErrorDescriptor PLATFORM_AUDIENCE_REQUIRES_PLATFORM_SCOPE =
      authorization("notification.platform_audience_requires_platform_scope");
  public static final ErrorDescriptor TENANT_AUDIENCE_REQUIRES_TENANT_SCOPE =
      authorization("notification.tenant_audience_requires_tenant_scope");
  public static final ErrorDescriptor RECIPIENTS_REQUIRED =
      validation("notification.recipients_required");
  public static final ErrorDescriptor SLACK_CHANNEL_KEY_REQUIRED =
      validation("notification.slack_channel_key_required");
  public static final ErrorDescriptor EMAIL_REQUIRED = validation("notification.email_required");
  public static final ErrorDescriptor EMAIL_INVALID = validation("notification.email_invalid");
  public static final ErrorDescriptor PHONE_REQUIRED = validation("notification.phone_required");
  public static final ErrorDescriptor PHONE_INVALID = validation("notification.phone_invalid");
  public static final ErrorDescriptor SEND_FAILED = serviceUnavailable("notification.send_failed");

  public static Set<ErrorDescriptor> all() {
    return Set.of(
        NOT_FOUND,
        PUBLICATION_NOT_FOUND,
        NOT_PUBLISHABLE,
        NOT_REPUBLISHABLE,
        REASON_REQUIRED,
        SYSTEM_KEYS_REQUIRED,
        TRANSLATION_REQUIRED,
        TRANSLATION_LOCALE_UNSUPPORTED,
        TENANT_REQUIRED,
        TARGET_REQUIRED,
        PLATFORM_AUDIENCE_REQUIRES_PLATFORM_SCOPE,
        TENANT_AUDIENCE_REQUIRES_TENANT_SCOPE,
        RECIPIENTS_REQUIRED,
        SLACK_CHANNEL_KEY_REQUIRED,
        EMAIL_REQUIRED,
        EMAIL_INVALID,
        PHONE_REQUIRED,
        PHONE_INVALID,
        SEND_FAILED);
  }

  private static ErrorDescriptor validation(String code) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.VALIDATION,
        HttpStatus.UNPROCESSABLE_ENTITY,
        ErrorRetryPolicy.AFTER_USER_ACTION,
        AUTHENTICATED_AUDIENCES,
        Set.of());
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

  private static ErrorDescriptor notFound(String code) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.NOT_FOUND,
        HttpStatus.NOT_FOUND,
        ErrorRetryPolicy.NEVER,
        AUTHENTICATED_AUDIENCES,
        Set.of());
  }

  private static ErrorDescriptor businessRule(String code) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.BUSINESS_RULE,
        HttpStatus.UNPROCESSABLE_ENTITY,
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
