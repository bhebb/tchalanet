package com.tchalanet.server.common.audit.web;

// common.audit.web.AuditLogAspect

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tchalanet.server.common.audit.domain.model.AuditAction;
import com.tchalanet.server.common.audit.domain.model.AuditEntityType;
import com.tchalanet.server.common.audit.domain.usecase.LogAuditEventUseCase;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogAspect {

  private final LogAuditEventUseCase auditUseCase;
  private final ObjectMapper objectMapper;

  private final ExpressionParser parser = new SpelExpressionParser();

  @Around("@annotation(com.tchalanet.server.common.audit.web.AuditLog)")
  public Object aroundAudit(ProceedingJoinPoint pjp) throws Throwable {
    Object result = null;
    Throwable error = null;

    try {
      result = pjp.proceed();
      return result;
    } catch (Throwable t) {
      error = t;
      throw t;
    } finally {
      try {
        handleAudit(pjp, result, error);
      } catch (Exception e) {
        log.warn("Audit logging failed", e);
      }
    }
  }

  private void handleAudit(ProceedingJoinPoint pjp, Object result, Throwable error) {
    MethodSignature sig = (MethodSignature) pjp.getSignature();
    var method = sig.getMethod();
    AuditLog ann = method.getAnnotation(AuditLog.class);
    if (ann == null) return;

    AuditEntityType entityType = ann.entity();
    AuditAction action = ann.action();

    // Contexte pour SpEL
    StandardEvaluationContext ctx = new StandardEvaluationContext();
    String[] paramNames = sig.getParameterNames();
    Object[] args = pjp.getArgs();
    for (int i = 0; i < paramNames.length; i++) {
      ctx.setVariable(paramNames[i], args[i]);
    }
    ctx.setVariable("result", result);
    ctx.setVariable("error", error);

    // id
    String entityId = "unknown";
    if (!ann.idExpression().isBlank()) {
      Expression expr = parser.parseExpression(ann.idExpression());
      Object v = expr.getValue(ctx);
      if (v != null) entityId = v.toString();
    }

    // details
    Map<String, Object> details = Map.of();
    if (!ann.detailsExpression().isBlank()) {
      Expression expr = parser.parseExpression(ann.detailsExpression());
      Object v = expr.getValue(ctx);
      // v peut être Map ou String JSON
      if (v != null) {
        if (v instanceof Map<?, ?> m) {
          // convert to Map<String,Object> using Class to avoid anonymous TypeReference
          @SuppressWarnings("unchecked")
          Map<String, Object> converted = objectMapper.convertValue(m, Map.class);
          details = converted;
        } else {
          details = Map.of("value", v.toString());
        }
      }
    }
    if (error != null) {
      details = new java.util.HashMap<>(details);
      details.put("error", error.getClass().getSimpleName());
      details.put("errorMessage", error.getMessage());
    }

    auditUseCase.log(entityType, entityId, action, details);
  }
}
