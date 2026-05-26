package com.tchalanet.server.platform.entitlement.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark an application entry point (e.g., a controller method)
 * as requiring a specific quota to be respected for the current tenant.
 * If the current usage exceeds the defined limit, a {@code ProblemRest.forbidden}
 * exception will be thrown.
 * <p>
 * This annotation relies on {@link EntitlementApi#requireLimitAtMost(com.tchalanet.server.common.types.id.TenantId, String, int)}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiredQuota {
    /**
     * The key of the feature associated with the quota.
     * This is often used for context or logging.
     */
    String feature() default "";

    /**
     * The key of the limit to check against.
     * E.g., "limits.terminals.max"
     */
    String limit();

    /**
     * The key or path to retrieve the current usage value.
     * This will be resolved dynamically at runtime.
     * E.g., "usage.terminals.active"
     */
    String usage();

    int increment() default 1;

}
