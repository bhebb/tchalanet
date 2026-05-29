package com.tchalanet.server.platform.entitlement.internal.aspect;

import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.platform.entitlement.api.EntitlementApi;
import com.tchalanet.server.platform.entitlement.api.RequiredFeature;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * Aspect that intercepts methods annotated with {@link RequiredFeature}
 * and checks if the current tenant has the required feature enabled.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class RequiredFeatureAspect {

    private final EntitlementApi entitlementApi;

    @Before("@annotation(requiredFeature) || @within(requiredFeature)")
    public void checkFeature(RequiredFeature requiredFeature) {
        var requestContext = TchContext.get();
        entitlementApi.requireFeature(requestContext.effectiveTenantIdRequired(), requiredFeature.value());
    }
}
