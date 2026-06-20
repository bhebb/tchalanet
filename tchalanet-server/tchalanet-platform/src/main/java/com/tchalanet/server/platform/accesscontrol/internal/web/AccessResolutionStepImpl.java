package com.tchalanet.server.platform.accesscontrol.internal.web;

import com.tchalanet.server.common.context.BootstrappedActor;
import com.tchalanet.server.common.context.ResolvedAccessContext;
import com.tchalanet.server.common.context.TchActorType;
import com.tchalanet.server.common.context.TchContextRequestAttributes;
import com.tchalanet.server.common.context.scope.ApiScope;
import com.tchalanet.server.common.context.web.ApiScopeResolver;
import com.tchalanet.server.common.http.TchHeaders;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.platform.accesscontrol.api.AccessResolutionStep;
import com.tchalanet.server.platform.accesscontrol.internal.service.AccessControlSnapshotResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccessResolutionStepImpl implements AccessResolutionStep {

    static final Set<String> SELLER_TERMINAL_PERMISSIONS = Set.of(
        "seller_terminal.me.read",
        "seller_terminal.sell",
        "seller_terminal.ticket.read_own",
        "seller_terminal.ticket.reprint_own"
    );

    private final AccessControlSnapshotResolver snapshotResolver;
    private final EffectiveTenantResolver effectiveTenantResolver;

    @Override
    public void resolve(HttpServletRequest request) {

        var bootstrappedActor = (BootstrappedActor)
            request.getAttribute(TchContextRequestAttributes.BOOTSTRAPPED_ACTOR);

        if (bootstrappedActor == null) {
            log.info("access.resolution no_bootstrapped_actor path={}", request.getRequestURI());
            // Public endpoint or unauthenticated — no actor to resolve
            return;
        }

        ResolvedAccessContext resolved;

        if (bootstrappedActor.isSellerTerminal()) {
            resolved = resolveSellerTerminal(request, bootstrappedActor);
        } else if (bootstrappedActor.isAppUser()) {
            resolved = resolveAppUser(request, bootstrappedActor);
        } else {
            throw ProblemRest.forbidden("unknown.actor_type");
        }

        request.setAttribute(TchContextRequestAttributes.RESOLVED_ACCESS, resolved);
        enrichSpringAuthentication(resolved);
    }

    // package-private for unit testing (avoids @Transactional proxy issue on private)
    ResolvedAccessContext resolveAppUser(HttpServletRequest request, BootstrappedActor actor) {
        var userId = actor.appUserId();
        var scope = ApiScopeResolver.resolve(request);

        // ADMIN / TENANT: primary business API — most frequent path.
        if (scope == ApiScope.ADMIN || scope == ApiScope.TENANT) {
            var platform = snapshotResolver.resolvePlatform(userId);
            var effectiveTenant = effectiveTenantResolver.resolveForAppUser(
                request, userId, platform.superAdmin(), platform.permissionKeys());
            var effectiveTenantId = effectiveTenant.tenantId();
            var roleCodes = new HashSet<>(platform.roleCodes());
            var permissionKeys = new HashSet<>(platform.permissionKeys());
            if (effectiveTenantId != null) {
                var tenantAccess = snapshotResolver.resolveTenant(userId, effectiveTenantId);
                roleCodes.addAll(tenantAccess.roleCodes());
                permissionKeys.addAll(tenantAccess.permissionKeys());
            }
            return new ResolvedAccessContext(
                TchActorType.APP_USER, userId, null, effectiveTenantId,
                platform.superAdmin(), effectiveTenant.tenantOverride(),
                roleCodes, permissionKeys);
        }

        // PLATFORM: platform roles only, no tenant context.
        if (scope == ApiScope.PLATFORM) {
            var platform = snapshotResolver.resolvePlatform(userId);
            return new ResolvedAccessContext(
                TchActorType.APP_USER, userId, null, null,
                platform.superAdmin(), false,
                platform.roleCodes(), platform.permissionKeys());
        }

        // IDENTITY / PUBLIC: authenticated user only — no DB queries needed.
        return ResolvedAccessContext.identityOnly(userId);
    }

    // package-private for unit testing
    ResolvedAccessContext resolveSellerTerminal(HttpServletRequest request, BootstrappedActor actor) {
        rejectTenantSelectionHeadersForTerminal(request);

        // A SellerTerminal is tenant-bound, not tenant-selecting: the tenant comes from the DB
        // mapping carried on the BootstrappedActor, never from a header.
        return new ResolvedAccessContext(
            TchActorType.SELLER_TERMINAL,
            null,
            actor.sellerTerminalId(),
            actor.tenantId(),
            false,
            false,
            Set.of(),
            SELLER_TERMINAL_PERMISSIONS
        );
    }

    private void rejectTenantSelectionHeadersForTerminal(HttpServletRequest request) {
        if (StringUtils.isNotBlank(request.getHeader(TchHeaders.X_TENANT_ID))
            || StringUtils.isNotBlank(request.getHeader(TchHeaders.X_TCH_TENANT_OVERRIDE))) {
            throw ProblemRest.forbidden("terminal.tenant_selection_not_allowed");
        }
    }

    private void enrichSpringAuthentication(ResolvedAccessContext resolved) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) return;

        var authorities = new ArrayList<GrantedAuthority>();

        // Actor type authority (ACTOR_APP_USER or ACTOR_SELLER_TERMINAL)
        authorities.add(new SimpleGrantedAuthority("ACTOR_" + resolved.actorType().name()));

        // DB-owned role authorities (ROLE_SUPER_ADMIN, ROLE_TENANT_ADMIN, etc.)
        for (var roleCode : resolved.roleCodes()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + roleCode));
        }

        // DB-owned permission authorities (PERM_seller_terminal.sell, PERM_ticket.void, etc.)
        for (var permKey : resolved.permissionKeys()) {
            authorities.add(new SimpleGrantedAuthority("PERM_" + permKey));
        }

        var enriched = new JwtAuthenticationToken(jwtAuth.getToken(), authorities);
        enriched.setDetails(jwtAuth.getDetails());
        SecurityContextHolder.getContext().setAuthentication(enriched);
        log.debug("access.resolved actorType={} roles={} permissions={}",
            resolved.actorType(), resolved.roleCodes(), resolved.permissionKeys());
    }
}
