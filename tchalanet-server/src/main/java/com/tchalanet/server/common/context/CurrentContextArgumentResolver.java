package com.tchalanet.server.common.context;

import static com.tchalanet.server.common.constant.ContextKeys.REQUEST_CONTEXT;

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

  private final TchRequestContextHolder requestContextHolder;

  @Override
  public boolean supportsParameter(MethodParameter p) {
    return p.hasParameterAnnotation(CurrentContext.class)
        && p.getParameterType().equals(TchRequestContext.class);
  }

  @Override
  public Object resolveArgument(
      MethodParameter p,
      ModelAndViewContainer mav,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {

    // 1) priorité au holder request-scope
    TchRequestContext ctx = requestContextHolder.get();
    if (ctx != null) {
      return ctx;
    }

    // 2) fallback sur l'attribut de requête (au cas où)
    HttpServletRequest req = webRequest.getNativeRequest(HttpServletRequest.class);
    if (req == null) {
      return null;
    }
    return req.getAttribute(REQUEST_CONTEXT);
  }
}
