package com.tchalanet.server.audit.web;

// common.audit.web.AuditLogAspect

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tchalanet.server.audit.application.command.model.RecordAuditEventCommand;
import com.tchalanet.server.audit.domain.ports.in.RecordAuditEventCommandHandler;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

@Aspect
@RequiredArgsConstructor
@Slf4j
public class AuditLogAspect {

  private final RecordAuditEventCommandHandler handler;
  private final ObjectMapper objectMapper;
  private final ExpressionParser parser = new SpelExpressionParser();

  @Around("@annotation(com.tchalanet.server.audit.infra.web.AuditLog)")
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
    var ann = method.getAnnotation(com.tchalanet.server.audit.infra.web.AuditLog.class);
    if (ann == null) return;

    var entityType = ann.entity();
    var action = ann.action();

    StandardEvaluationContext ctx = new StandardEvaluationContext();
    String[] paramNames = sig.getParameterNames();
    Object[] args = pjp.getArgs();
    for (int i = 0; i < paramNames.length; i++) {
      ctx.setVariable(paramNames[i], args[i]);
    }
    ctx.setVariable("result", result);
    ctx.setVariable("error", error);

    // entityId
    String entityId = "unknown";
    if (!ann.idExpression().isBlank()) {
      Object v = parser.parseExpression(ann.idExpression()).getValue(ctx);
      if (v != null) entityId = v.toString();
    }

    // details
    Map<String, Object> details = Map.of();
    if (!ann.detailsExpression().isBlank()) {
      Object v = parser.parseExpression(ann.detailsExpression()).getValue(ctx);
      if (v != null) {
        if (v instanceof Map<?, ?> m) {
          @SuppressWarnings("unchecked")
          Map<String, Object> converted = objectMapper.convertValue(m, Map.class);
          details = converted;
        } else {
          details = Map.of("value", v.toString());
        }
      }
    }
    if (error != null) {
      var mut = new java.util.HashMap<>(details);
      mut.put("error", error.getClass().getSimpleName());
      mut.put("errorMessage", error.getMessage());
      details = mut;
    }

    handler.handle(new RecordAuditEventCommand(entityType, entityId, action, details));
  }
}
