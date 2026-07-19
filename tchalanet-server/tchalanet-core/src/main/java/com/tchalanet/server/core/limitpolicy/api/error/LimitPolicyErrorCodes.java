package com.tchalanet.server.core.limitpolicy.api.error;

import com.tchalanet.server.common.web.error.ErrorAudience;
import com.tchalanet.server.common.web.error.ErrorCategory;
import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorRetryPolicy;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;

/** Stable client-facing limit policy failures. */
@UtilityClass
public class LimitPolicyErrorCodes {

  private static final Set<ErrorAudience> ADMIN_AUDIENCES = Set.of(ErrorAudience.WEB_ADMIN);

  public static final ErrorDescriptor LIMIT_BLOCKED =
      new ErrorDescriptor(
          "limits.blocked",
          ErrorCategory.BUSINESS_RULE,
          HttpStatus.FORBIDDEN,
          ErrorRetryPolicy.AFTER_USER_ACTION,
          Set.of(ErrorAudience.WEB_ADMIN, ErrorAudience.MOBILE),
          Set.of());
  public static final ErrorDescriptor TARGET_TYPE_REQUIRED =
      validation("limits.target_type_required");
  public static final ErrorDescriptor TARGET_TYPE_UNSUPPORTED =
      validation("limits.target_type_unsupported");
  public static final ErrorDescriptor TARGET_ID_REQUIRED = validation("limits.target_id_required");
  public static final ErrorDescriptor TARGET_ID_INVALID = validation("limits.target_id_invalid");

  public static Set<ErrorDescriptor> all() {
    return Set.of(
        LIMIT_BLOCKED,
        TARGET_TYPE_REQUIRED,
        TARGET_TYPE_UNSUPPORTED,
        TARGET_ID_REQUIRED,
        TARGET_ID_INVALID);
  }

  private static ErrorDescriptor validation(String code) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.VALIDATION,
        HttpStatus.UNPROCESSABLE_CONTENT,
        ErrorRetryPolicy.AFTER_USER_ACTION,
        ADMIN_AUDIENCES,
        Set.of());
  }
}
