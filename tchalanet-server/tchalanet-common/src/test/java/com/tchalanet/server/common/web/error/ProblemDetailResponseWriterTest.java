package com.tchalanet.server.common.web.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.web.api.CommonErrorDescriptors;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ProblemDetailResponseWriterTest {

  @Test
  void writesStructuredRedactedAuthenticationFailure() throws Exception {
    var request = new MockHttpServletRequest("GET", "/api/v1/tenant/dashboard");
    var response = new MockHttpServletResponse();

    ProblemDetailResponseWriter.write(
        request, response, CommonErrorDescriptors.AUTHENTICATION_REQUIRED);

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentType()).startsWith("application/problem+json");
    assertThat(response.getContentAsString())
        .contains("\"code\":\"access.authentication_required\"")
        .contains("\"category\":\"auth_required\"")
        .contains("\"retryPolicy\":\"AFTER_REAUTH\"")
        .contains("\"retryable\":true")
        .doesNotContain("token expired")
        .doesNotContain("Exception");
  }
}
