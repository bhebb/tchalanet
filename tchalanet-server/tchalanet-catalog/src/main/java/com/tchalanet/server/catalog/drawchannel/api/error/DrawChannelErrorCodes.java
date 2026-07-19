package com.tchalanet.server.catalog.drawchannel.api.error;

import com.tchalanet.server.common.web.error.ErrorAudience;
import com.tchalanet.server.common.web.error.ErrorCategory;
import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorRetryPolicy;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;

/** Stable client-facing errors for platform draw-channel administration. */
@UtilityClass
public class DrawChannelErrorCodes {

  private static final Set<ErrorAudience> PLATFORM_AUDIENCES = Set.of(ErrorAudience.WEB_PLATFORM);

  public static final ErrorDescriptor DELETED = notFound("catalog.drawchannel.deleted");
  public static final ErrorDescriptor FLAGS_REQUIRED =
      validation("catalog.drawchannel.flags_required");
  public static final ErrorDescriptor FLAGS_INVALID =
      validation("catalog.drawchannel.flags_invalid");

  public static Set<ErrorDescriptor> all() {
    return Set.of(DELETED, FLAGS_REQUIRED, FLAGS_INVALID);
  }

  private static ErrorDescriptor notFound(String code) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.NOT_FOUND,
        HttpStatus.NOT_FOUND,
        ErrorRetryPolicy.NEVER,
        PLATFORM_AUDIENCES,
        Set.of());
  }

  private static ErrorDescriptor validation(String code) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.VALIDATION,
        HttpStatus.BAD_REQUEST,
        ErrorRetryPolicy.AFTER_USER_ACTION,
        PLATFORM_AUDIENCES,
        Set.of());
  }
}
