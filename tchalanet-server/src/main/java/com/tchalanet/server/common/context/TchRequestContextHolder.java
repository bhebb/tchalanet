package com.tchalanet.server.common.context;

import static com.tchalanet.server.common.constant.ContextKeys.REQUEST_CONTEXT;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class TchRequestContextHolder {
  private final TchRequestContext ctx;

  public TchRequestContextHolder(HttpServletRequest req) {
    this.ctx = (TchRequestContext) req.getAttribute(REQUEST_CONTEXT);
  }

  public TchRequestContext get() {
    return ctx;
  }
}
