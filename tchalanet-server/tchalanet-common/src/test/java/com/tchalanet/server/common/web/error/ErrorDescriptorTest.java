package com.tchalanet.server.common.web.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ErrorDescriptorTest {

  @Test
  void codeFirstProblemCarriesStableMetadataWithoutMessageInference() {
    var descriptor =
        new ErrorDescriptor(
            "sales.ticket.closed", ErrorCategory.BUSINESS_RULE, ErrorRetryPolicy.AFTER_USER_ACTION);

    var problem = ProblemRest.conflict(descriptor).getProblem();

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(problem.getDetail()).isEqualTo("sales.ticket.closed");
    assertThat(problem.getProperties())
        .containsEntry("code", "sales.ticket.closed")
        .containsEntry("category", "BUSINESS_RULE")
        .containsEntry("retryPolicy", "AFTER_USER_ACTION");
  }

  @Test
  void rejectsProseAndDynamicErrorCodeShapes() {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                new ErrorDescriptor(
                    "Draw is not open", ErrorCategory.BUSINESS_RULE, ErrorRetryPolicy.NEVER));
  }
}
