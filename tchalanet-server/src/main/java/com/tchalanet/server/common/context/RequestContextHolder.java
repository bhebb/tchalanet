package com.tchalanet.server.common.context;

import static com.tchalanet.server.common.domain.AppConstants.REQUEST_CONTEXT;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class RequestContextHolder {
  private final TchRequestContext ctx;

  public RequestContextHolder(HttpServletRequest req) {
    this.ctx = (TchRequestContext) req.getAttribute(REQUEST_CONTEXT);
  }

  public TchRequestContext get() {
    return ctx;
  }
}
