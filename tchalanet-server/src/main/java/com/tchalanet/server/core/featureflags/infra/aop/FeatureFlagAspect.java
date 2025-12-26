package com.tchalanet.server.core.featureflags.infra.aop;

import com.tchalanet.server.common.context.TchRequestContextHolder;
import com.tchalanet.server.core.featureflags.application.annotation.FeatureFlagEnabled;
import com.tchalanet.server.core.featureflags.domain.model.FeatureContext;
import com.tchalanet.server.core.featureflags.domain.ports.in.IsFeatureEnabledQuery;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.id.TerminalId;
import java.util.UUID;
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
public class FeatureFlagAspect {

  private final IsFeatureEnabledQuery isFeatureEnabledQuery;
  private final ExpressionParser parser = new SpelExpressionParser();
  private final TchRequestContextHolder tchRequestContextHolder;

  @Around("@annotation(featureFlagEnabled)")
  public Object checkFeatureFlag(
      ProceedingJoinPoint joinPoint, FeatureFlagEnabled featureFlagEnabled) throws Throwable {
    String flagKey = featureFlagEnabled.value();
    FeatureContext context = buildFeatureContext(joinPoint, featureFlagEnabled);

    if (!isFeatureEnabledQuery.isEnabled(flagKey, context)) {
      throw new FeatureDisabledException(
          "Feature flag '" + flagKey + "' is disabled for the current context.");
    }

    return joinPoint.proceed();
  }

  private FeatureContext buildFeatureContext(
      ProceedingJoinPoint joinPoint, FeatureFlagEnabled featureFlagEnabled) {
    var requestContext = tchRequestContextHolder.get();
    TenantId tenantId = requestContext != null && requestContext.tenantUuid() != null ? TenantId.of(requestContext.tenantUuid()) : null;
    UserId userId = requestContext != null && requestContext.userUuid() != null ? UserId.of(requestContext.userUuid()) : null;
    TerminalId terminalId = null;

    // Use SpEL to extract tenantId, userId, terminalId from method arguments if provided in
    // annotation
    StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
    MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
    String[] parameterNames = methodSignature.getParameterNames();
    Object[] args = joinPoint.getArgs();

    for (int i = 0; i < parameterNames.length; i++) {
      evaluationContext.setVariable(parameterNames[i], args[i]);
    }

    if (!featureFlagEnabled.tenantIdSpEL().isBlank()) {
      var raw = parser.parseExpression(featureFlagEnabled.tenantIdSpEL()).getValue(evaluationContext, Object.class);
      if (raw instanceof UUID u) tenantId = TenantId.of(u);
      else if (raw instanceof String s) tenantId = TenantId.of(java.util.UUID.fromString(s));
    }
    if (!featureFlagEnabled.userIdSpEL().isBlank()) {
      var raw = parser.parseExpression(featureFlagEnabled.userIdSpEL()).getValue(evaluationContext, Object.class);
      if (raw instanceof UUID u) userId = UserId.of(u);
      else if (raw instanceof String s) userId = UserId.of(java.util.UUID.fromString(s));
    }
    if (!featureFlagEnabled.terminalIdSpEL().isBlank()) {
      var raw = parser.parseExpression(featureFlagEnabled.terminalIdSpEL()).getValue(evaluationContext, Object.class);
      if (raw instanceof UUID u) terminalId = TerminalId.of(u);
      else if (raw instanceof String s) terminalId = TerminalId.of(java.util.UUID.fromString(s));
    }

    if (tenantId == null) {
      throw new IllegalStateException(
          "TenantId could not be resolved for feature flag evaluation. Ensure it's in TchRequestContext or provided via SpEL.");
    }

    return new FeatureContext(
        tenantId, userId, terminalId, null); // custom properties can be added via SpEL
  }

  public static class FeatureDisabledException extends RuntimeException {
    public FeatureDisabledException(String message) {
      super(message);
    }
  }
}
