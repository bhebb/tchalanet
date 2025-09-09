package com.tchalanet.server.config.context;

import static com.tchalanet.server.constants.AppConstants.REQUEST_CONTEXT;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class RequestContextHolder {
  private final RequestContext ctx;

  public RequestContextHolder(HttpServletRequest req) {
    this.ctx = (RequestContext) req.getAttribute(REQUEST_CONTEXT);
  }

  public RequestContext get() {
    return ctx;
  }
}
