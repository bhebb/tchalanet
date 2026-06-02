package com.tchalanet.server.platform.entitlement.internal.aspect;

import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.platform.entitlement.api.EntitlementApi;
import com.tchalanet.server.platform.entitlement.api.RequiredFeature;
import java.lang.reflect.Method;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

/**
 * Aspect that intercepts methods annotated with {@link RequiredFeature}
 * (or whose declaring class is annotated) and checks the current tenant has
 * the required feature enabled.
 *
 * <p>The annotation is resolved from the join point rather than bound as an
 * advice parameter: a pointcut of the form
 * {@code @annotation(x) || @within(x)} cannot reliably bind {@code x} in
 * Spring AOP (when only one branch matches the other binds {@code null}),
 * which previously caused an NPE on every {@code @RequiredFeature} endpoint.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class RequiredFeatureAspect {

    private final EntitlementApi entitlementApi;

    @Before("@annotation(com.tchalanet.server.platform.entitlement.api.RequiredFeature)"
        + " || @within(com.tchalanet.server.platform.entitlement.api.RequiredFeature)")
    public void checkFeature(JoinPoint joinPoint) {
        RequiredFeature requiredFeature = resolveAnnotation(joinPoint);
        if (requiredFeature == null) {
            return; // nothing to enforce
        }
        var requestContext = TchContext.get();
        entitlementApi.requireFeature(
            requestContext.effectiveTenantIdRequired(), requiredFeature.value());
    }

    /** Method-level annotation wins; otherwise fall back to the declaring class. */
    private RequiredFeature resolveAnnotation(JoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        RequiredFeature onMethod =
            AnnotatedElementUtils.findMergedAnnotation(method, RequiredFeature.class);
        if (onMethod != null) {
            return onMethod;
        }
        Class<?> targetClass =
            joinPoint.getTarget() != null ? joinPoint.getTarget().getClass()
                : method.getDeclaringClass();
        return AnnotatedElementUtils.findMergedAnnotation(targetClass, RequiredFeature.class);
    }
}
