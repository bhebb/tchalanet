package com.tchalanet.server.features.pos.tickets.error;

import com.tchalanet.server.common.web.error.ErrorCategory;
import com.tchalanet.server.common.web.error.ErrorAudience;
import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorRetryPolicy;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;

/** Stable API errors owned by the cashier ticket-receipt feature. */
@UtilityClass
public class PosTicketReceiptErrorCodes {

  public static final ErrorDescriptor PRINT_OPTIONS_INVALID =
      new ErrorDescriptor(
          "pos.receipt.print_options_invalid",
          ErrorCategory.VALIDATION,
          HttpStatus.BAD_REQUEST,
          ErrorRetryPolicy.AFTER_USER_ACTION,
          Set.of(ErrorAudience.WEB_ADMIN, ErrorAudience.MOBILE),
          Set.of());

  public static Set<ErrorDescriptor> all() {
    return Set.of(PRINT_OPTIONS_INVALID);
  }
}
