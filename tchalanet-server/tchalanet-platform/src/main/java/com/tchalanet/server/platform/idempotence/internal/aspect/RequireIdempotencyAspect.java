package com.tchalanet.server.platform.idempotence.internal.aspect;

import com.tchalanet.server.common.constant.TchHeaders;
import com.tchalanet.server.platform.idempotence.api.RequireIdempotency;
import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.platform.idempotence.api.IdempotencyStore;
import com.tchalanet.server.platform.idempotence.internal.service.RequestHasher;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
public class RequireIdempotencyAspect {

  private final IdempotencyStore store;
  private final JsonUtils jsonUtils;

  @Around("@annotation(com.tchalanet.server.platform.idempotence.api.RequireIdempotency) || @within(com.tchalanet.server.platform.idempotence.api.RequireIdempotency)")
  public Object around(ProceedingJoinPoint pjp) throws Throwable {
    var sig = (MethodSignature) pjp.getSignature();
    RequireIdempotency ann = sig.getMethod().getAnnotation(RequireIdempotency.class);
    if (ann == null) {
      // maybe the annotation is on the class
      ann = pjp.getTarget().getClass().getAnnotation(RequireIdempotency.class);
    }

    HttpServletRequest req = currentRequest();
    String key = req != null ? req.getHeader(TchHeaders.IDEMPOTENCY_KEY) : null;
    if (StringUtils.isBlank(key)) {
      throw ProblemRest.badRequest("idempotency.missing");
    }
    key = key.trim();

    Object body = findRequestBodyArg(pjp.getArgs()).orElse(null);
    String hash = RequestHasher.sha256Normalized(jsonUtils, body);

    var begin = store.begin(ann.scope(), key, hash, ann.ttlSeconds());

    return switch (begin.decision()) {
      case PAYLOAD_MISMATCH -> throw ProblemRest.conflict("idempotency.payload_mismatch");
      case IN_PROGRESS -> throw ProblemRest.conflict("idempotency.in_progress");
      case ALREADY_COMPLETED -> {
        var c = begin.completed().orElseThrow();
        if (c.responseJson() != null) {
          yield jsonUtils.readValue(c.responseJson(), Object.class);
        }
        throw ProblemRest.conflict("idempotency.completed_no_response");
      }
      case STARTED -> pjp.proceed();
    };
  }

  private Optional<Object> findRequestBodyArg(Object[] args) {
    if (args == null || args.length == 0) return Optional.empty();
    return java.util.Arrays.stream(args)
        .filter(a -> a != null)
        .filter(a -> !(a instanceof jakarta.servlet.ServletRequest))
        .filter(a -> !(a instanceof jakarta.servlet.ServletResponse))
        .findFirst();
  }

  private HttpServletRequest currentRequest() {
    var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attrs == null) return null;
    return attrs.getRequest();
  }
}
