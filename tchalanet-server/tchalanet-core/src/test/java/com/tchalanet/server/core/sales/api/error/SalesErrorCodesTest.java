package com.tchalanet.server.core.sales.api.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.web.error.ErrorCategory;
import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorRetryPolicy;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class SalesErrorCodesTest {

  @Test
  void registersPriorityPosCodesWithClientSafeContract() {
    assertThat(SalesErrorCodes.all())
        .hasSize(50)
        .anySatisfy(
            descriptor(
                "sales.draw.cutoff_elapsed",
                ErrorCategory.CONFLICT,
                HttpStatus.CONFLICT,
                ErrorRetryPolicy.AFTER_USER_ACTION))
        .anySatisfy(
            descriptor(
                "sales.seller_terminal.cannot_sell",
                ErrorCategory.BUSINESS_RULE,
                HttpStatus.FORBIDDEN,
                ErrorRetryPolicy.AFTER_USER_ACTION))
        .anySatisfy(
            descriptor(
                "sales.preparation.expired",
                ErrorCategory.CONFLICT,
                HttpStatus.CONFLICT,
                ErrorRetryPolicy.AFTER_USER_ACTION))
        .anySatisfy(
            descriptor(
                "ticket.reprint.reason_required",
                ErrorCategory.VALIDATION,
                HttpStatus.BAD_REQUEST,
                ErrorRetryPolicy.AFTER_USER_ACTION));
  }

  private static Consumer<ErrorDescriptor> descriptor(
      String code, ErrorCategory category, HttpStatus status, ErrorRetryPolicy retryPolicy) {
    return error -> {
      assertThat(error.code()).isEqualTo(code);
      assertThat(error.category()).isEqualTo(category);
      assertThat(error.expectedStatus()).isEqualTo(status);
      assertThat(error.retryPolicy()).isEqualTo(retryPolicy);
      assertThat(error.publicParams()).isEmpty();
    };
  }
}
