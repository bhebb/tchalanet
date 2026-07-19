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

  public static final ErrorDescriptor LIMIT_BLOCKED =
      new ErrorDescriptor(
          "limits.blocked",
          ErrorCategory.BUSINESS_RULE,
          HttpStatus.FORBIDDEN,
          ErrorRetryPolicy.AFTER_USER_ACTION,
          Set.of(ErrorAudience.WEB_ADMIN, ErrorAudience.MOBILE),
          Set.of());

  public static Set<ErrorDescriptor> all() {
    return Set.of(LIMIT_BLOCKED);
  }
}
