package com.tchalanet.server.platform.idempotence.internal.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.common.http.TchHeaders;
import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.platform.idempotence.api.RequireIdempotency;
import com.tchalanet.server.platform.idempotence.api.error.IdempotencyErrorCodes;
import com.tchalanet.server.platform.idempotence.api.model.IdempotencyScope;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

class RequireIdempotencyInterceptorTest {

  @Test
  void missingIdempotencyKeyUsesTheStructuredErrorContract() throws Exception {
    var request = new MockHttpServletRequest();
    var response = new MockHttpServletResponse();
    var handler = new HandlerMethod(new DummyController(), "sell");

    assertThatThrownBy(
            () -> new RequireIdempotencyInterceptor().preHandle(request, response, handler))
        .isInstanceOf(ProblemRestException.class)
        .satisfies(error -> {
          var problem = ((ProblemRestException) error).getProblem();
          assertThat(problem.getProperties())
              .containsEntry("code", IdempotencyErrorCodes.MISSING.code());
        });

    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
  }

  @Test
  void presentKeyIsTrimmedAndStoredOnTheRequest() throws Exception {
    var request = new MockHttpServletRequest();
    request.addHeader(TchHeaders.IDEMPOTENCY_KEY, "  key-1  ");
    var response = new MockHttpServletResponse();
    var handler = new HandlerMethod(new DummyController(), "sell");

    var allowed = new RequireIdempotencyInterceptor().preHandle(request, response, handler);

    assertThat(allowed).isTrue();
    assertThat(request.getAttribute(RequireIdempotencyInterceptor.ATTR_IDEM_KEY))
        .isEqualTo("key-1");
  }

  private static final class DummyController {
    @RequireIdempotency(scope = IdempotencyScope.SALES_SELL_TICKET)
    public void sell() {}
  }
}
