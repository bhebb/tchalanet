package com.tchalanet.server.platform.idempotence.api.error;

import com.tchalanet.server.common.web.error.ErrorAudience;
import com.tchalanet.server.common.web.error.ErrorCategory;
import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorRetryPolicy;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;

@UtilityClass
public class IdempotencyErrorCodes {

  private static final Set<ErrorAudience> PRIVATE_AUDIENCES =
      Set.of(ErrorAudience.WEB_ADMIN, ErrorAudience.WEB_PLATFORM, ErrorAudience.MOBILE);

  public static final ErrorDescriptor MISSING =
      new ErrorDescriptor(
          "idempotency.missing",
          ErrorCategory.VALIDATION,
          HttpStatus.BAD_REQUEST,
          ErrorRetryPolicy.AFTER_USER_ACTION,
          PRIVATE_AUDIENCES,
          Set.of());

  public static final ErrorDescriptor PAYLOAD_MISMATCH = conflict("idempotency.payload_mismatch");
  public static final ErrorDescriptor IN_PROGRESS = conflict("idempotency.in_progress");
  public static final ErrorDescriptor COMPLETED_NO_RESPONSE =
      conflict("idempotency.completed_no_response");

  public static Set<ErrorDescriptor> all() {
    return Set.of(MISSING, PAYLOAD_MISMATCH, IN_PROGRESS, COMPLETED_NO_RESPONSE);
  }

  private static ErrorDescriptor conflict(String code) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.CONFLICT,
        HttpStatus.CONFLICT,
        ErrorRetryPolicy.RETRY_SAME_INTENT,
        PRIVATE_AUDIENCES,
        Set.of());
  }
}
