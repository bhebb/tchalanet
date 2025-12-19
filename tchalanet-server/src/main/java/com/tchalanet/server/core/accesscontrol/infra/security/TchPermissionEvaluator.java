package com.tchalanet.server.core.accesscontrol.infra.security;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.core.accesscontrol.application.query.handler.CheckUserPermissionsHandler;
import com.tchalanet.server.core.accesscontrol.application.query.model.CheckUserPermissionsQuery;
import com.tchalanet.server.core.accesscontrol.domain.exception.PermissionsDeniedException;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
@Component
@Slf4j
public class TchPermissionEvaluator implements PermissionEvaluator {

    private final CheckUserPermissionsHandler checkUserPermissionsUseCase;

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
        var hasPermission = false;

        var ctx = prepareContext(authentication, permission);
        if (ctx == null) {
            return false;
        }

        UUID tenantId = ctx.principal().tenantUuid();
        UUID userId = ctx.principal().userUuid();
        String permissionKey = ctx.permissionKey();

        try {
            checkUserPermissionsUseCase.handle(new CheckUserPermissionsQuery(tenantId, userId, Set.of(permissionKey)));
            hasPermission = true;
        } catch (PermissionsDeniedException ex) {
            log.debug(
                "Permission denied by domain: tenant={} user={} perm= {}",
                tenantId,
                userId,
                permissionKey,
                ex);
        }
        return hasPermission;
    }

    /**
     * Helper record that holds the principal and the normalized permission key.
     */
    private record PermissionEvalContext(TchRequestContext principal, String permissionKey) {
    }

    /**
     * Validate input authentication and permission, returning a context with the principal and
     * permission key. Returns null when validation fails (logs are emitted by this method).
     */
    private PermissionEvalContext prepareContext(Authentication authentication, Object permission) {
        if (authentication == null
            || !(authentication.getPrincipal() instanceof TchRequestContext principal)) {
            log.warn("Permission check denied: no valid principal");
            return null;
        }

        if (permission == null) {
            log.warn("Permission check denied: null permission");
            return null;
        }

        String permissionKey = permission.toString().trim();
        if (permissionKey.isEmpty()) {
            log.warn("Permission check denied: blank permission");
            return null;
        }

        return new PermissionEvalContext(principal, permissionKey);
    }
}
