package com.tchalanet.server.common.context.web;

import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.common.context.TchRequestContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static com.tchalanet.server.common.constant.ContextKeys.REQUEST_CONTEXT;

@Component
@RequiredArgsConstructor
public class CurrentContextArgumentResolver implements HandlerMethodArgumentResolver {

  @Override
  public boolean supportsParameter(MethodParameter p) {
    return (p.hasParameterAnnotation(CurrentContext.class)
        || p.hasParameterAnnotation(CurrentContext.class))
        && p.getParameterType().equals(TchRequestContext.class);
  }

  @Override
  public Object resolveArgument(
      MethodParameter p,
      ModelAndViewContainer mav,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {

    // 1) Fallback request attribute (works even if ThreadLocal not set for some reason)
    HttpServletRequest req = webRequest.getNativeRequest(HttpServletRequest.class);
    if (req != null) {
      Object attr = req.getAttribute(REQUEST_CONTEXT);
      if (attr instanceof TchRequestContext ctx) {
        return ctx;
      }
    }

    // 2) ThreadLocal (normal path: set by TchContextFilter)
    return TchContext.currentOrNull();
  }
}
