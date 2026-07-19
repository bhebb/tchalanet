package com.tchalanet.server.catalog.game.api.error;

import com.tchalanet.server.common.web.error.ErrorAudience;
import com.tchalanet.server.common.web.error.ErrorCategory;
import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorRetryPolicy;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;

/** Stable client-facing errors for platform game catalog administration. */
@UtilityClass
public class GameCatalogErrorCodes {

  private static final Set<ErrorAudience> PLATFORM_AUDIENCES = Set.of(ErrorAudience.WEB_PLATFORM);

  public static final ErrorDescriptor NOT_FOUND = notFound("catalog.game.not_found");
  public static final ErrorDescriptor STRUCTURAL_UPDATE_IN_USE =
      conflict("catalog.game.structural_update_in_use");
  public static final ErrorDescriptor DELETE_IN_USE = conflict("catalog.game.delete_in_use");

  public static Set<ErrorDescriptor> all() {
    return Set.of(NOT_FOUND, STRUCTURAL_UPDATE_IN_USE, DELETE_IN_USE);
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

  private static ErrorDescriptor conflict(String code) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.CONFLICT,
        HttpStatus.CONFLICT,
        ErrorRetryPolicy.NEVER,
        PLATFORM_AUDIENCES,
        Set.of());
  }
}
