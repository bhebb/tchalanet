package com.tchalanet.server.catalog.resultslot.api.error;

import com.tchalanet.server.common.web.error.ErrorAudience;
import com.tchalanet.server.common.web.error.ErrorCategory;
import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorRetryPolicy;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;

/** Stable client-facing errors for platform result-slot administration. */
@UtilityClass
public class ResultSlotErrorCodes {

  private static final Set<ErrorAudience> PLATFORM_AUDIENCES = Set.of(ErrorAudience.WEB_PLATFORM);

  public static final ErrorDescriptor NOT_FOUND = notFound("catalog.resultslot.not_found");
  public static final ErrorDescriptor CONFIG_INVALID =
      validation("catalog.resultslot.config_invalid");
  public static final ErrorDescriptor GAME_KEY_INVALID =
      validation("catalog.resultslot.game_key_invalid");

  public static Set<ErrorDescriptor> all() {
    return Set.of(NOT_FOUND, CONFIG_INVALID, GAME_KEY_INVALID);
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
