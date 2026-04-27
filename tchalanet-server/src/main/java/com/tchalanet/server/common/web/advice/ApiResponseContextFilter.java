package com.tchalanet.server.common.web.advice;

import jakarta.servlet.*;
import java.io.IOException;

/** Servlet filter that clears the ApiResponseContext ThreadLocal at the end of each request. */
public class ApiResponseContextFilter implements Filter {

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    try {
      chain.doFilter(request, response);
    } finally {
      ApiResponseContext.clear();
    }
  }
}
