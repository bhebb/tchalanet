package com.tchalanet.server.features.pos.tickets.error;

import com.tchalanet.server.common.web.error.ErrorAudience;
import com.tchalanet.server.common.web.error.ErrorCategory;
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

  public static final ErrorDescriptor DELIVERY_CHANNEL_REQUIRED =
      validation("pos.receipt.delivery_channel_required");
  public static final ErrorDescriptor DELIVERY_RECIPIENT_REQUIRED =
      validation("pos.receipt.delivery_recipient_required");
  public static final ErrorDescriptor ATTACHMENT_TOO_LARGE =
      validation("pos.receipt.attachment_too_large");
  public static final ErrorDescriptor SELLER_TERMINAL_REQUIRED =
      new ErrorDescriptor(
          "pos.receipt.seller_terminal_required",
          ErrorCategory.AUTHORIZATION,
          HttpStatus.FORBIDDEN,
          ErrorRetryPolicy.AFTER_REAUTH,
          Set.of(ErrorAudience.WEB_ADMIN, ErrorAudience.MOBILE),
          Set.of());

  public static Set<ErrorDescriptor> all() {
    return Set.of(
        PRINT_OPTIONS_INVALID,
        DELIVERY_CHANNEL_REQUIRED,
        DELIVERY_RECIPIENT_REQUIRED,
        ATTACHMENT_TOO_LARGE,
        SELLER_TERMINAL_REQUIRED);
  }

  private static ErrorDescriptor validation(String code) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.VALIDATION,
        HttpStatus.UNPROCESSABLE_ENTITY,
        ErrorRetryPolicy.AFTER_USER_ACTION,
        Set.of(ErrorAudience.WEB_ADMIN, ErrorAudience.MOBILE),
        Set.of());
  }
}
