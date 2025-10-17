package com.tchalanet.server.config.context;

import static com.tchalanet.server.constants.AppConstants.REQUEST_CONTEXT;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CurrentContextArgumentResolver implements HandlerMethodArgumentResolver {

  @Override
  public boolean supportsParameter(MethodParameter p) {
    return p.hasParameterAnnotation(CurrentContext.class)
        && p.getParameterType().equals(RequestContext.class);
  }

  @Override
  public Object resolveArgument(
      MethodParameter p, ModelAndViewContainer m, NativeWebRequest w, WebDataBinderFactory b) {
    return w.getNativeRequest(HttpServletRequest.class).getAttribute(REQUEST_CONTEXT);
  }
}
