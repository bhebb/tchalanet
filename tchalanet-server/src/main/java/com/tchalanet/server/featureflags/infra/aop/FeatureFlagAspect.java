package com.tchalanet.server.featureflags.infra.aop;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.TchRequestContextHolder;
import com.tchalanet.server.featureflags.application.annotation.FeatureFlagEnabled;
import com.tchalanet.server.featureflags.domain.model.FeatureContext;
import com.tchalanet.server.featureflags.domain.ports.in.IsFeatureEnabledQuery;
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
    TchRequestContext requestContext = TchRequestContextHolder.getCurrentContext();
    UUID tenantId = requestContext != null ? requestContext.tenantUuid() : null;
    UUID userId =
        (requestContext != null
                && requestContext.userId() != null
                && !requestContext.userId().isBlank())
            ? UUID.fromString(requestContext.userId())
            : null;

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
      tenantId =
          parser
              .parseExpression(featureFlagEnabled.tenantIdSpEL())
              .getValue(evaluationContext, UUID.class);
    }
    if (!featureFlagEnabled.userIdSpEL().isBlank()) {
      userId =
          parser
              .parseExpression(featureFlagEnabled.userIdSpEL())
              .getValue(evaluationContext, UUID.class);
    }
    if (!featureFlagEnabled.terminalIdSpEL().isBlank()) {
      // Assuming terminalId is a UUID, adjust if it's a String
      userId =
          parser
              .parseExpression(featureFlagEnabled.terminalIdSpEL())
              .getValue(evaluationContext, UUID.class);
    }

    if (tenantId == null) {
      throw new IllegalStateException(
          "TenantId could not be resolved for feature flag evaluation. Ensure it's in TchRequestContext or provided via SpEL.");
    }

    return new FeatureContext(
        tenantId, userId, null, null); // TerminalId and custom properties can be added via SpEL
  }

  public static class FeatureDisabledException extends RuntimeException {
    public FeatureDisabledException(String message) {
      super(message);
    }
  }
}
