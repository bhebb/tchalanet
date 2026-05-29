package com.tchalanet.server.platform.entitlement.internal.aspect;

import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.platform.entitlement.api.EntitlementApi;
import com.tchalanet.server.platform.entitlement.api.RequiredQuota;
import com.tchalanet.server.platform.entitlement.internal.UsageResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * Aspect that intercepts methods annotated with {@link RequiredQuota}
 * and checks if the current tenant respects the defined quota.
 * The 'usage' value is resolved by delegating to {@link UsageResolver}.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class RequiredQuotaAspect {


    private final EntitlementApi entitlementApi;
    private final UsageResolver usageResolver;

    @Before("@annotation(requiredQuota)")
    public void checkQuota(RequiredQuota requiredQuota) {
        var ctx = TchContext.get();
        var tenantId = ctx.effectiveTenantIdRequired();

        int currentUsage = usageResolver.currentUsage(tenantId, requiredQuota.usage());
        int requestedUsage = currentUsage + requiredQuota.increment();

        entitlementApi.requireLimitAtMost(
            tenantId,
            requiredQuota.limit(),
            requestedUsage
        );
    }
}
