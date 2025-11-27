package com.tchalanet.server.featureflags.application.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a method as requiring a feature flag to be enabled. If the feature flag is
 * disabled, a FeatureDisabledException will be thrown.
 *
 * <p>Usage: @FeatureFlagEnabled("ff.new_feature")
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface FeatureFlagEnabled {
  String value(); // The key of the feature flag, e.g., "ff.new_feature"

  String tenantIdSpEL() default ""; // SpEL expression to extract tenantId from method arguments

  String userIdSpEL() default ""; // SpEL expression to extract userId from method arguments

  String terminalIdSpEL() default ""; // SpEL expression to extract terminalId from method arguments
}
