package com.tchalanet.server.platform.accesscontrol.internal.permissionevaluator;

import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.platform.accesscontrol.internal.exception.PermissionsDeniedException;
import com.tchalanet.server.platform.accesscontrol.internal.service.DefaultAccessControlService;
import jakarta.annotation.Nullable;
import java.io.Serializable;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TchPermissionEvaluator implements PermissionEvaluator {

    private final TchContextResolver contextResolver;
    private final DefaultAccessControlService service;

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
            service.checkPermissions(
                new com.tchalanet.server.platform.accesscontrol.api.model.request.CheckUserPermissionsRequest(
                    eval.tenantId(), eval.userId(), Set.of(eval.permissionKey())));
            return false;
        } catch (PermissionsDeniedException ex) {
            log.debug(
                "Permission denied by domain: tenant={} user={} permission={}",
                eval.tenantId(),
                eval.userId(),
                eval.permissionKey(),
                ex);
            return false;
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
