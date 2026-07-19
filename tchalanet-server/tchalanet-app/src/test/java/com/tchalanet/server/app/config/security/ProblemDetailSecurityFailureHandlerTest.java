package com.tchalanet.server.app.config.security;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.tchalanet.server.common.web.observability.RequiredRequestIdFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

class ProblemDetailSecurityFailureHandlerTest {

  private final ProblemDetailSecurityFailureHandler handler =
      new ProblemDetailSecurityFailureHandler();
  private final Logger logger =
      (Logger) LoggerFactory.getLogger(ProblemDetailSecurityFailureHandler.class);
  private final ListAppender<ILoggingEvent> logs = new ListAppender<>();

  @AfterEach
  void detachLogAppender() {
    logger.detachAppender(logs);
  }

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

  @Test
  void authenticationFailureWritesCorrelatedRedactedSecurityLog() throws Exception {
    logs.start();
    logger.addAppender(logs);
    var request = new MockHttpServletRequest("POST", "/api/v1/tenant/sales/tickets");
    request.setAttribute(RequiredRequestIdFilter.ATTR_REQUEST_ID, "req-security-test");

    handler.commence(
        request,
        new MockHttpServletResponse(),
        new BadCredentialsException("token revoked for seller terminal POS-001"));

    assertThat(logs.list)
        .singleElement()
        .extracting(ILoggingEvent::getFormattedMessage)
        .satisfies(
            message ->
                assertThat(message)
                    .contains("security.request.rejected")
                    .contains("code=access.authentication_required")
                    .contains("requestId=req-security-test")
                    .doesNotContain("token revoked")
                    .doesNotContain("POS-001"));
  }
}
