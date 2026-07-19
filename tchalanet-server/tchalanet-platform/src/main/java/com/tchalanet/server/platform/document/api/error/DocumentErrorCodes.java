package com.tchalanet.server.platform.document.api.error;

import com.tchalanet.server.common.web.error.ErrorAudience;
import com.tchalanet.server.common.web.error.ErrorCategory;
import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorRetryPolicy;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;

/** Stable client-facing failures for document rendering. */
@UtilityClass
public class DocumentErrorCodes {

  private static final Set<ErrorAudience> AUDIENCES =
      Set.of(ErrorAudience.WEB_ADMIN, ErrorAudience.WEB_PLATFORM, ErrorAudience.MOBILE);

  public static final ErrorDescriptor REQUEST_INVALID = validation("document.request_invalid");
  public static final ErrorDescriptor RENDERER_UNAVAILABLE =
      serviceUnavailable("document.renderer_unavailable");
  public static final ErrorDescriptor RENDER_FAILED = serviceUnavailable("document.render_failed");

  public static Set<ErrorDescriptor> all() {
    return Set.of(REQUEST_INVALID, RENDERER_UNAVAILABLE, RENDER_FAILED);
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
