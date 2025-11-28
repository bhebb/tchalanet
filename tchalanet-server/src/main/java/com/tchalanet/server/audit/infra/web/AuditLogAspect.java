package com.tchalanet.server.audit.infra.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tchalanet.server.audit.application.command.model.LogAuditEventCommand;
import com.tchalanet.server.audit.application.port.in.LogAuditEventCommandHandler;
import com.tchalanet.server.audit.domain.model.AuditAction;
import com.tchalanet.server.audit.domain.model.AuditEntityType;
import java.util.HashMap;
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
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogAspect {

  private final LogAuditEventCommandHandler handler;
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
        // l'audit ne doit jamais casser le flux métier
        log.warn("Audit logging failed", e);
      }
    }
  }

  private void handleAudit(ProceedingJoinPoint pjp, Object result, Throwable error) {
    MethodSignature signature = (MethodSignature) pjp.getSignature();
    AuditLog annotation = signature.getMethod().getAnnotation(AuditLog.class);
    if (annotation == null) {
      return;
    }

    AuditEntityType entityType = annotation.entity();
    AuditAction action = annotation.action();

    StandardEvaluationContext ctx = buildEvaluationContext(signature, pjp.getArgs(), result, error);

    String entityId = resolveEntityId(annotation, ctx);
    Map<String, Object> details = resolveDetails(annotation, ctx, error);

    handler.handle(new LogAuditEventCommand(entityType, entityId, action, details));
  }

  private StandardEvaluationContext buildEvaluationContext(
      MethodSignature signature, Object[] args, Object result, Throwable error) {

    StandardEvaluationContext ctx = new StandardEvaluationContext();

    String[] paramNames = signature.getParameterNames();
    if (paramNames != null) {
      for (int i = 0; i < paramNames.length; i++) {
        ctx.setVariable(paramNames[i], args[i]);
      }
    }

    ctx.setVariable("result", result);
    ctx.setVariable("error", error);
    return ctx;
  }

  private String resolveEntityId(AuditLog annotation, StandardEvaluationContext ctx) {
    String expr = annotation.idExpression();
    if (expr == null || expr.isBlank()) {
      return "unknown";
    }
    Object value = parser.parseExpression(expr).getValue(ctx);
    return value != null ? value.toString() : "unknown";
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> resolveDetails(
      AuditLog annotation, StandardEvaluationContext ctx, Throwable error) {

    Map<String, Object> details = Map.of();
    String expr = annotation.detailsExpression();

    if (expr != null && !expr.isBlank()) {
      Object v = parser.parseExpression(expr).getValue(ctx);
      if (v != null) {
        if (v instanceof Map<?, ?> m) {
          details = objectMapper.convertValue(m, Map.class);
        } else {
          details = Map.of("value", v.toString());
        }
      }
    }

    if (error != null) {
      Map<String, Object> mutable = new HashMap<>(details);
      mutable.put("error", error.getClass().getSimpleName());
      mutable.put("errorMessage", error.getMessage());
      details = mutable;
    }

    return details;
  }
}
