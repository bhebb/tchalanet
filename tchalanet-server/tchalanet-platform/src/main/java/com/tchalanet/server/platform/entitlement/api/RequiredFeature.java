package com.tchalanet.server.platform.entitlement.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark an application entry point (e.g., a controller method)
 * as requiring a specific feature to be enabled for the current tenant.
 * If the feature is not enabled, a {@code ProblemRest.forbidden} exception will be thrown.
 *
 * This annotation relies on {@link EntitlementApi#requireFeature(com.tchalanet.server.common.types.id.TenantId, String)}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiredFeature {
    /**
     * The key of the feature that is required.
     * E.g., "promotion.rules.basic"
     */
    String value();
}
