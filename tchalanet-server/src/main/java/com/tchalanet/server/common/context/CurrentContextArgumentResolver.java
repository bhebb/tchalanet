package com.tchalanet.server.common.context;

import static com.tchalanet.server.common.domain.AppConstants.REQUEST_CONTEXT;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
public class CurrentContextArgumentResolver implements HandlerMethodArgumentResolver {

  private final RequestContextHolder requestContextHolder;

  @Override
  public boolean supportsParameter(MethodParameter p) {
    return p.hasParameterAnnotation(CurrentContext.class)
        && p.getParameterType().equals(TchRequestContext.class);
  }

  @Override
  public Object resolveArgument(
      MethodParameter p, ModelAndViewContainer m, NativeWebRequest w, WebDataBinderFactory b) {
    TchRequestContext ctx = requestContextHolder.get();
    if (ctx != null) return ctx;
    HttpServletRequest req = w.getNativeRequest(HttpServletRequest.class);
    if (req == null) return null;
    return req.getAttribute(REQUEST_CONTEXT);
  }
}
