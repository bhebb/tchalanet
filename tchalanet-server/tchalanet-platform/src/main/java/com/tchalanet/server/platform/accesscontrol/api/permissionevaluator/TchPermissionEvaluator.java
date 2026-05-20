package com.tchalanet.server.platform.accesscontrol.api.permissionevaluator;

import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.platform.accesscontrol.api.AccessControlApi;
import com.tchalanet.server.platform.accesscontrol.api.model.request.CheckUserPermissionsRequest;
import jakarta.annotation.Nullable;
import java.io.Serializable;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TchPermissionEvaluator implements PermissionEvaluator {
    private static final Logger log = LoggerFactory.getLogger(TchPermissionEvaluator.class);

    private final TchContextResolver contextResolver;
    private final AccessControlApi accessControlApi;

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
        var eval = prepareContext(authentication, permission);
        if (eval == null) {
            return false;
        }

        try {
            var result =
                accessControlApi.checkPermissions(
                    new CheckUserPermissionsRequest(
                        eval.tenantId(), eval.userId(), Set.of(eval.permissionKey())));
            return result.allowed();
        } catch (RuntimeException ex) {
            log.warn(
                "Permission evaluation failed: tenant={} user={} permission={}",
                eval.tenantId(),
                eval.userId(),
                eval.permissionKey(),
                ex);
            return false;
        }
    }

    private PermissionEvalContext prepareContext(Authentication authentication, Object permission) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Permission check denied: unauthenticated request");
            return null;
        }

        TchRequestContext ctx = contextResolver.currentOrNull();
        if (ctx == null) {
            log.warn("Permission check denied: no TchRequestContext bound");
            return null;
        }

        String permissionKey = normalizePermission(permission);
        if (permissionKey == null) {
            return null;
        }

        TenantId tenantId = ctx.tenantIdSafe();
        if (tenantId == null) {
            log.warn("Permission check denied: missing tenant context permission={}", permissionKey);
            return null;
        }

        UserId userId;
        try {
            userId = ctx.currentUserIdRequired();
        } catch (RuntimeException ex) {
            log.warn("Permission check denied: missing app user permission={}", permissionKey);
            return null;
        }

        return new PermissionEvalContext(tenantId, userId, permissionKey);
    }

    private String normalizePermission(Object permission) {
        if (permission == null) {
            log.warn("Permission check denied: null permission");
            return null;
        }

        String permissionKey = permission.toString().trim();
        if (permissionKey.isEmpty()) {
            log.warn("Permission check denied: blank permission");
            return null;
        }

        return permissionKey;
    }

    private record PermissionEvalContext(TenantId tenantId, UserId userId, String permissionKey) {}
}
