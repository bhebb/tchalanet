package com.tchalanet.server.platform.communication.api.error;

import com.tchalanet.server.common.web.error.ErrorAudience;
import com.tchalanet.server.common.web.error.ErrorCategory;
import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorRetryPolicy;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;

/** Stable client-facing failures for outbound communication. */
@UtilityClass
public class CommunicationErrorCodes {

  private static final Set<ErrorAudience> AUDIENCES =
      Set.of(ErrorAudience.WEB_ADMIN, ErrorAudience.WEB_PLATFORM, ErrorAudience.MOBILE);

  public static final ErrorDescriptor REQUEST_INVALID = validation("communication.request_invalid");
  public static final ErrorDescriptor CHANNEL_DISABLED =
      businessRule("communication.channel_disabled");
  public static final ErrorDescriptor ENQUEUE_FAILED =
      serviceUnavailable("communication.enqueue_failed");
  public static final ErrorDescriptor PROVIDER_UNAVAILABLE =
      serviceUnavailable("communication.provider_unavailable");
  public static final ErrorDescriptor DELIVERY_FAILED =
      serviceUnavailable("communication.delivery_failed");

  public static Set<ErrorDescriptor> all() {
    return Set.of(
        REQUEST_INVALID, CHANNEL_DISABLED, ENQUEUE_FAILED, PROVIDER_UNAVAILABLE, DELIVERY_FAILED);
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

  private static ErrorDescriptor businessRule(String code) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.BUSINESS_RULE,
        HttpStatus.UNPROCESSABLE_ENTITY,
        ErrorRetryPolicy.AFTER_USER_ACTION,
        AUDIENCES,
        Set.of());
  }

  private static ErrorDescriptor serviceUnavailable(String code) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.SERVICE_UNAVAILABLE,
        HttpStatus.SERVICE_UNAVAILABLE,
        ErrorRetryPolicy.AFTER_DELAY,
        AUDIENCES,
        Set.of());
  }
}
