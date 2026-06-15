package com.tchalanet.server.platform.accesscontrol.api.permissionevaluator;

import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.context.TchRequestContext;
import jakarta.annotation.Nullable;
import java.io.Serializable;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Evaluates {@code @PreAuthorize("hasPermission(...)")} expressions against the
 * permission set already resolved by {@code AccessResolutionFilter} and stored in
 * {@link TchRequestContext#permissionKeys()}.
 *
 * <p>No repository calls — all DB work is done upstream in the filter pipeline.
 */
@Component
@RequiredArgsConstructor
public class TchPermissionEvaluator implements PermissionEvaluator {
    private static final Logger log = LoggerFactory.getLogger(TchPermissionEvaluator.class);

    private final TchContextResolver contextResolver;

    @Override
    public boolean hasPermission(
        @Nullable Authentication authentication,
        @Nullable Object targetDomainObject,
        @Nullable Object permission) {
        return evaluate(authentication, permission);
    }

    @Override
    public boolean hasPermission(
        @Nullable Authentication authentication,
        @Nullable Serializable targetId,
        @Nullable String targetType,
        @Nullable Object permission) {
        return evaluate(authentication, permission);
    }

    private boolean evaluate(Authentication authentication, Object permission) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Permission check denied: unauthenticated request");
            return false;
        }

        TchRequestContext ctx = contextResolver.currentOrNull();
        if (ctx == null) {
            log.warn("Permission check denied: no TchRequestContext bound");
            return false;
        }

        String permKey = normalizePermission(permission);
        if (permKey == null) {
            return false;
        }

        return ctx.permissionKeys().contains(permKey);
    }

    private String normalizePermission(Object permission) {
        if (permission == null) {
            log.warn("Permission check denied: null permission");
            return null;
        }
        String permKey = permission.toString().trim();
        if (permKey.isEmpty()) {
            log.warn("Permission check denied: blank permission");
            return null;
        }
        return permKey;
    }
}
