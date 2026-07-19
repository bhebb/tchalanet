package com.tchalanet.server.core.drawresult.api.error;

import com.tchalanet.server.common.web.error.ErrorAudience;
import com.tchalanet.server.common.web.error.ErrorCategory;
import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorRetryPolicy;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;

/** Stable client-facing errors for globally sourced draw results. */
@UtilityClass
public class DrawResultErrorCodes {

  private static final Set<ErrorAudience> RESULT_AUDIENCES =
      Set.of(
          ErrorAudience.WEB_PUBLIC,
          ErrorAudience.WEB_ADMIN,
          ErrorAudience.WEB_PLATFORM,
          ErrorAudience.MOBILE);

  public static final ErrorDescriptor NOT_FOUND = notFound("drawresult.not_found");
  public static final ErrorDescriptor RESULT_SLOT_NOT_FOUND =
      notFound("drawresult.result_slot.not_found");
  public static final ErrorDescriptor OVERRIDE_SETTLED_FORBIDDEN =
      conflict("drawresult.override_settled_forbidden");
  public static final ErrorDescriptor INVALID_EXTERNAL_PICK =
      unprocessable("drawresult.external_pick_invalid", ErrorRetryPolicy.NEVER);
  public static final ErrorDescriptor PROJECTION_FAILED =
      unavailable("drawresult.projection_failed");

  public static Set<ErrorDescriptor> all() {
    return Set.of(
        NOT_FOUND,
        RESULT_SLOT_NOT_FOUND,
        OVERRIDE_SETTLED_FORBIDDEN,
        INVALID_EXTERNAL_PICK,
        PROJECTION_FAILED);
  }

  private static ErrorDescriptor notFound(String code) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.NOT_FOUND,
        HttpStatus.NOT_FOUND,
        ErrorRetryPolicy.NEVER,
        RESULT_AUDIENCES,
        Set.of());
  }

  private static ErrorDescriptor conflict(String code) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.CONFLICT,
        HttpStatus.CONFLICT,
        ErrorRetryPolicy.NEVER,
        RESULT_AUDIENCES,
        Set.of());
  }

  private static ErrorDescriptor unprocessable(String code, ErrorRetryPolicy retryPolicy) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.VALIDATION,
        HttpStatus.UNPROCESSABLE_CONTENT,
        retryPolicy,
        RESULT_AUDIENCES,
        Set.of());
  }

  private static ErrorDescriptor unavailable(String code) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.SERVICE_UNAVAILABLE,
        HttpStatus.SERVICE_UNAVAILABLE,
        ErrorRetryPolicy.AFTER_DELAY,
        RESULT_AUDIENCES,
        Set.of());
  }
}
