package com.tchalanet.server.platform.audit.internal.web;

import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.platform.audit.api.AuditApi;
import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import com.tchalanet.server.platform.audit.api.AuditLog;
import com.tchalanet.server.platform.audit.api.model.request.LogAuditEventRequest;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;

@Aspect
@Component
@Slf4j
public class AuditLogAspect {

    private final AuditApi auditApi;
    private final JsonUtils jsonUtils;
    private final TchContextResolver contextResolver;

    private final ExpressionParser parser = new SpelExpressionParser();

    public AuditLogAspect(AuditApi auditApi, JsonUtils jsonUtils) {
        this(auditApi, jsonUtils, null);
    }

    @Autowired
    public AuditLogAspect(
        AuditApi auditApi, JsonUtils jsonUtils, TchContextResolver contextResolver) {
        this.auditApi = auditApi;
        this.jsonUtils = jsonUtils;
        this.contextResolver = contextResolver;
    }

    @Around("@annotation(com.tchalanet.server.platform.audit.api.AuditLog) || @within(com.tchalanet.server.platform.audit.api.AuditLog)")
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
                var cmd = buildCommandOrNull(pjp, result, error);
                if (cmd != null) {
                    // SUCCESS => log AFTER COMMIT (only if transaction commits)
                    if (error == null) {
                        AfterCommit.run(() -> safeSend(cmd));
                    } else {
                        // ERROR => log immediately in REQUIRES_NEW (handler decides)
                        safeSend(cmd);
                    }
                }
            } catch (Exception e) {
                // audit must never break business flow
                log.warn("Audit logging failed", e);
            }
        }
    }

    private void safeSend(LogAuditEventRequest cmd) {
        try {
            auditApi.logAuditEvent(cmd);
        } catch (Exception e) {
            // never fail the main operation
            log.warn("Audit command send failed", e);
        }
    }

    private LogAuditEventRequest buildCommandOrNull(
        ProceedingJoinPoint pjp, Object result, Throwable error) {

        MethodSignature signature = (MethodSignature) pjp.getSignature();
        AuditLog annotation = resolveAnnotation(signature, pjp.getTarget());
        if (annotation == null) return null;

        AuditEntityType entityType = annotation.entity();
        AuditAction action = annotation.action();

        StandardEvaluationContext ctx =
            buildEvaluationContext(signature, pjp.getArgs(), result, error);

        String entityId = resolveEntityId(annotation, ctx);
        Map<String, Object> details = resolveDetails(annotation, ctx, error);
        UUID tenantId = resolveTenantId(annotation, ctx);

        return new LogAuditEventRequest(entityType, entityId, action, details, tenantId);
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
        if (expr == null || expr.isBlank()) return "unknown";

        Object value = parser.parseExpression(expr).getValue(ctx);
        return value != null ? value.toString() : "unknown";
    }

    private UUID resolveTenantId(AuditLog annotation, StandardEvaluationContext ctx) {
        String expr = annotation.tenantIdExpression();
        if (expr == null || expr.isBlank()) return null;

        Object value = parser.parseExpression(expr).getValue(ctx);
        if (value == null) return null;
        if (value instanceof UUID uuid) return uuid;
        if (value instanceof com.tchalanet.server.common.types.id.TenantId tenantId) {
            return tenantId.uuid();
        }
        return UUID.fromString(value.toString());
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
                    // normalize to Map<String,Object> (json roundtrip is OK if you want canonical JSON)
                    details = jsonUtils.convertValue(m, new TypeReference<Map<String, Object>>() {});
                } else {
                    details = normalizeObjectDetails(v);
                }
            }
        }

        Map<String, Object> mutable = new HashMap<>(details);
        addRequestIdIfAvailable(mutable);
        mutable.put("outcome", error == null ? "SUCCESS" : "FAIL");
        if (error != null) {
            mutable.put("error", error.getClass().getSimpleName());
            mutable.put("errorMessage", error.getMessage());
        }

        return mutable;
    }

    private void addRequestIdIfAvailable(Map<String, Object> details) {
        if (contextResolver == null || details.containsKey("requestId")) {
            return;
        }
        try {
            var ctx = contextResolver.currentOrNull();
            if (ctx != null && ctx.requestId() != null && !ctx.requestId().isBlank()) {
                details.put("requestId", ctx.requestId());
            }
        } catch (Exception e) {
            log.debug("Unable to resolve audit requestId", e);
        }
    }

    private Map<String, Object> normalizeObjectDetails(Object value) {
        try {
            return jsonUtils.convertValue(value, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ignored) {
            // Avoid leaking problematic object graphs (can fail with ClassCastException during JSON serialization).
            return Map.of(
                "value", String.valueOf(value),
                "valueType", value == null ? "null" : value.getClass().getName());
        }
    }

    private AuditLog resolveAnnotation(MethodSignature signature, Object target) {
        Method method = signature.getMethod();
        AuditLog annotation = method.getAnnotation(AuditLog.class);
        if (annotation != null) {
            return annotation;
        }
        Class<?> targetClass = target != null ? target.getClass() : method.getDeclaringClass();
        return targetClass.getAnnotation(AuditLog.class);
    }
}
