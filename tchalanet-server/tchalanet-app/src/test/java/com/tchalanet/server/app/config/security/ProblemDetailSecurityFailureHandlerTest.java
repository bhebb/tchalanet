package com.tchalanet.server.app.config.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;

class ProblemDetailSecurityFailureHandlerTest {

  private final ProblemDetailSecurityFailureHandler handler =
      new ProblemDetailSecurityFailureHandler();

  @Test
  void authenticationFailureUsesGenericStableContract() throws Exception {
    var response = new MockHttpServletResponse();

    handler.commence(
        new MockHttpServletRequest("POST", "/api/v1/tenant/sales/tickets"),
        response,
        new BadCredentialsException("token revoked for seller terminal POS-001"));

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentAsString())
        .contains("\"code\":\"access.authentication_required\"")
        .doesNotContain("token revoked")
        .doesNotContain("POS-001");
  }

  @Test
  void authorizationFailureUsesStableContract() throws Exception {
    var response = new MockHttpServletResponse();

    handler.handle(
        new MockHttpServletRequest("POST", "/api/v1/platform/tenants"),
        response,
        new AccessDeniedException("tenant 123 is blocked"));

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getContentAsString())
        .contains("\"code\":\"access.denied\"")
        .doesNotContain("tenant 123")
        .doesNotContain("blocked");
  }
}
