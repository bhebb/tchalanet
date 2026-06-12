package com.tchalanet.server.common.web.observability;

import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Routes exceptions raised before DispatcherServlet through the application's exception handlers.
 *
 * <p>Security and identity filters execute outside MVC, so {@code @ControllerAdvice} cannot catch
 * their exceptions directly. Resolving them here preserves the active request/trace context and
 * produces the same diagnostic ProblemDetail as controller failures.
 */
@RequiredArgsConstructor
public class ServletExceptionBridgeFilter extends OncePerRequestFilter {

  private final HandlerExceptionResolver exceptionResolver;

  @Override
  protected void doFilterInternal(
      @Nonnull HttpServletRequest request,
      @Nonnull HttpServletResponse response,
      @Nonnull FilterChain filterChain)
      throws ServletException, IOException {
    try {
      filterChain.doFilter(request, response);
    } catch (Exception exception) {
      if (response.isCommitted()
          || exceptionResolver.resolveException(request, response, null, exception) == null) {
        rethrow(exception);
      }
    }
  }

  private static void rethrow(Exception exception) throws ServletException, IOException {
    if (exception instanceof IOException ioException) {
      throw ioException;
    }
    if (exception instanceof ServletException servletException) {
      throw servletException;
    }
    throw new ServletException(exception);
  }
}
