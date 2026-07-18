package com.tchalanet.server.features.pos.tickets.error;

import com.tchalanet.server.common.web.error.ErrorCategory;
import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorRetryPolicy;
import lombok.experimental.UtilityClass;

/** Stable API errors owned by the cashier ticket-receipt feature. */
@UtilityClass
public class PosTicketReceiptErrorCodes {

  public static final ErrorDescriptor PRINT_OPTIONS_INVALID =
      new ErrorDescriptor(
          "pos.receipt.print_options_invalid",
          ErrorCategory.VALIDATION,
          ErrorRetryPolicy.AFTER_USER_ACTION);
}
