package com.tchalanet.server.core.draw.api.error;

import com.tchalanet.server.common.web.error.ErrorAudience;
import com.tchalanet.server.common.web.error.ErrorCategory;
import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorRetryPolicy;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;

/** Stable client-facing draw lifecycle and result-correction errors. */
@UtilityClass
public class DrawErrorCodes {

  private static final Set<ErrorAudience> OPS_AUDIENCES =
      Set.of(ErrorAudience.WEB_ADMIN, ErrorAudience.WEB_PLATFORM);

  public static final ErrorDescriptor CORRECT_RESULT_REASON_REQUIRED =
      validation("draw.correct_result.reason_required");
  public static final ErrorDescriptor CORRECT_RESULT_REASON_TOO_SHORT =
      validation("draw.correct_result.reason_too_short");
  public static final ErrorDescriptor CORRECT_RESULT_IDEMPOTENCY_KEY_REQUIRED =
      validation("draw.correct_result.idempotency_key_required");
  public static final ErrorDescriptor CORRECT_RESULT_NO_RESULT_APPLIED =
      conflict("draw.correct_result.no_result_applied");
  public static final ErrorDescriptor CORRECT_RESULT_SAME_RESULT =
      conflict("draw.correct_result.same_result");
  public static final ErrorDescriptor LOOKAHEAD_DAYS_INVALID =
      validation("draw.lookahead_days_invalid");
  public static final ErrorDescriptor SCHEDULE_INVALID = validation("draw.schedule_invalid");
  public static final ErrorDescriptor LIFECYCLE_DRAW_IDS_REQUIRED =
      validation("draw.lifecycle.draw_ids_required");
  public static final ErrorDescriptor LIFECYCLE_DRAW_IDS_LIMIT_EXCEEDED =
      validation("draw.lifecycle.draw_ids_limit_exceeded");

  public static Set<ErrorDescriptor> all() {
    return Set.of(
        CORRECT_RESULT_REASON_REQUIRED,
        CORRECT_RESULT_REASON_TOO_SHORT,
        CORRECT_RESULT_IDEMPOTENCY_KEY_REQUIRED,
        CORRECT_RESULT_NO_RESULT_APPLIED,
        CORRECT_RESULT_SAME_RESULT,
        LOOKAHEAD_DAYS_INVALID,
        SCHEDULE_INVALID,
        LIFECYCLE_DRAW_IDS_REQUIRED,
        LIFECYCLE_DRAW_IDS_LIMIT_EXCEEDED);
  }

  private static ErrorDescriptor validation(String code) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.VALIDATION,
        HttpStatus.BAD_REQUEST,
        ErrorRetryPolicy.AFTER_USER_ACTION,
        OPS_AUDIENCES,
        Set.of());
  }

  private static ErrorDescriptor conflict(String code) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.CONFLICT,
        HttpStatus.CONFLICT,
        ErrorRetryPolicy.AFTER_USER_ACTION,
        OPS_AUDIENCES,
        Set.of());
  }
}
