package com.tchalanet.server.core.sellerterminal.api.error;

import com.tchalanet.server.common.web.error.ErrorAudience;
import com.tchalanet.server.common.web.error.ErrorCategory;
import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorRetryPolicy;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;

/** Stable client-facing failures owned by seller-terminal management. */
@UtilityClass
public class SellerTerminalErrorCodes {

  private static final Set<ErrorAudience> AUDIENCES =
      Set.of(ErrorAudience.WEB_ADMIN, ErrorAudience.MOBILE);

  public static final ErrorDescriptor NOT_FOUND = notFound("sellerterminal.not_found");
  public static final ErrorDescriptor STATUS_TRANSITION_INVALID =
      conflict("sellerterminal.status_transition_invalid");
  public static final ErrorDescriptor COMMISSION_RATE_INVALID =
      validation("sellerterminal.commission_rate_invalid");
  public static final ErrorDescriptor IDENTITY_NOT_BOUND =
      conflict("sellerterminal.identity_not_bound");
  public static final ErrorDescriptor IDENTITY_PROVISION_UNAVAILABLE =
      serviceUnavailable("sellerterminal.identity_provision_unavailable");
  public static final ErrorDescriptor PIN_RESET_UNAVAILABLE =
      serviceUnavailable("sellerterminal.pin_reset_unavailable");

  public static Set<ErrorDescriptor> all() {
    return Set.of(
        NOT_FOUND,
        STATUS_TRANSITION_INVALID,
        COMMISSION_RATE_INVALID,
        IDENTITY_NOT_BOUND,
        IDENTITY_PROVISION_UNAVAILABLE,
        PIN_RESET_UNAVAILABLE);
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

  private static ErrorDescriptor notFound(String code) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.NOT_FOUND,
        HttpStatus.NOT_FOUND,
        ErrorRetryPolicy.NEVER,
        AUDIENCES,
        Set.of());
  }

  private static ErrorDescriptor conflict(String code) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.CONFLICT,
        HttpStatus.CONFLICT,
        ErrorRetryPolicy.AFTER_USER_ACTION,
        AUDIENCES,
        Set.of());
  }

  private static ErrorDescriptor serviceUnavailable(String code) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.SERVICE_UNAVAILABLE,
        HttpStatus.BAD_GATEWAY,
        ErrorRetryPolicy.AFTER_DELAY,
        AUDIENCES,
        Set.of());
  }
}
