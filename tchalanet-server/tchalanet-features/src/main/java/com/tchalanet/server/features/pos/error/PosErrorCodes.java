package com.tchalanet.server.features.pos.error;

import com.tchalanet.server.common.web.error.ErrorAudience;
import com.tchalanet.server.common.web.error.ErrorCategory;
import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorRetryPolicy;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;

/** Stable client-facing failures owned by the POS feature surface. */
@UtilityClass
public class PosErrorCodes {

  private static final Set<ErrorAudience> AUDIENCES =
      Set.of(ErrorAudience.WEB_ADMIN, ErrorAudience.MOBILE);

  public static final ErrorDescriptor SURFACE_NOT_ALLOWED =
      authorization("pos.surface.not_allowed");
  public static final ErrorDescriptor SELLER_TERMINAL_REQUIRED =
      authorization("pos.home.seller_terminal_required");
  public static final ErrorDescriptor SELLER_TERMINAL_MISMATCH =
      authorization("pos.tickets.seller_terminal_mismatch");
  public static final ErrorDescriptor DASHBOARD_ANALYTICS_UNAVAILABLE =
      unavailable("pos.dashboard.analytics_unavailable");

  public static Set<ErrorDescriptor> all() {
    return Set.of(
        SURFACE_NOT_ALLOWED,
        SELLER_TERMINAL_REQUIRED,
        SELLER_TERMINAL_MISMATCH,
        DASHBOARD_ANALYTICS_UNAVAILABLE);
  }

  private static ErrorDescriptor authorization(String code) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.AUTHORIZATION,
        HttpStatus.FORBIDDEN,
        ErrorRetryPolicy.AFTER_REAUTH,
        AUDIENCES,
        Set.of());
  }

  private static ErrorDescriptor unavailable(String code) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.SERVICE_UNAVAILABLE,
        HttpStatus.SERVICE_UNAVAILABLE,
        ErrorRetryPolicy.RETRY_SAME_INTENT,
        AUDIENCES,
        Set.of());
  }
}
