package com.tchalanet.server.features.pagemodel.security;

import com.tchalanet.server.common.security.TchRole;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares which roles are allowed to invoke this {@link
 * com.tchalanet.server.core.pagemodel.api.dynamic.PageModelDynamicProvider}.
 *
 * <p>Providers without this annotation are assumed public (no role restriction). Providers with
 * this annotation will have their invocation rejected with a {@code dynamic.error} entry (not a
 * 500) if the current role is not in the allowed set.
 *
 * <p>[harden-pagemodel-security-v2 / D2] Provider-level revalidation — second line of defence
 * after {@link PageModelAccessPolicy}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PageModelAllowedRoles {
  TchRole[] value();
}
