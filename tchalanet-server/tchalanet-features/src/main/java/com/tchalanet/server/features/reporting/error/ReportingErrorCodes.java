package com.tchalanet.server.features.reporting.error;

import com.tchalanet.server.common.web.error.ErrorAudience;
import com.tchalanet.server.common.web.error.ErrorCategory;
import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorRetryPolicy;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;

/** Stable degradations owned by the tenant-admin reporting surface. */
@UtilityClass
public class ReportingErrorCodes {

  public static final ErrorDescriptor ANALYTICS_UNAVAILABLE =
      new ErrorDescriptor(
          "reporting.analytics.unavailable",
          ErrorCategory.SERVICE_UNAVAILABLE,
          HttpStatus.SERVICE_UNAVAILABLE,
          ErrorRetryPolicy.RETRY_SAME_INTENT,
          Set.of(ErrorAudience.WEB_ADMIN, ErrorAudience.MOBILE),
          Set.of());

  public static Set<ErrorDescriptor> all() {
    return Set.of(ANALYTICS_UNAVAILABLE);
  }
}
