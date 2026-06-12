package com.tchalanet.server.common.web.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

class ServletExceptionBridgeFilterTest {

  @Test
  void resolvesExceptionRaisedBeforeDispatcherServlet() throws Exception {
    var resolver = mock(HandlerExceptionResolver.class);
    when(resolver.resolveException(any(), any(), any(), any()))
        .thenAnswer(
            invocation -> {
              var response = invocation.getArgument(1, MockHttpServletResponse.class);
              response.setStatus(500);
              response.addHeader("X-Trace-Id", "trace-123");
              return new ModelAndView();
            });
    var filter = new ServletExceptionBridgeFilter(resolver);
    var response = new MockHttpServletResponse();
    FilterChain chain = (request, res) -> {
      throw new IllegalStateException("failed before dispatcher");
    };

    filter.doFilter(new MockHttpServletRequest(), response, chain);

    assertThat(response.getStatus()).isEqualTo(500);
    assertThat(response.getHeader("X-Trace-Id")).isEqualTo("trace-123");
  }

  @Test
  void rethrowsUnresolvedException() {
    var resolver = mock(HandlerExceptionResolver.class);
    var filter = new ServletExceptionBridgeFilter(resolver);
    FilterChain chain = (request, response) -> {
      throw new IllegalStateException("unresolved");
    };

    assertThatThrownBy(
            () ->
                filter.doFilter(
                    new MockHttpServletRequest(), new MockHttpServletResponse(), chain))
        .isInstanceOf(ServletException.class)
        .hasRootCauseMessage("unresolved");
  }
}
