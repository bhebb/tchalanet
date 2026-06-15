package com.tchalanet.server.platform.accesscontrol.internal.web;

import com.tchalanet.server.common.context.BootstrappedActor;
import com.tchalanet.server.common.context.ResolvedAccessContext;
import com.tchalanet.server.common.context.TchActorType;
import com.tchalanet.server.common.context.TchContextRequestAttributes;
import com.tchalanet.server.common.context.scope.ApiScope;
import com.tchalanet.server.common.context.web.ApiScopeResolver;
import com.tchalanet.server.common.http.TchHeaders;
import com.tchalanet.server.common.types.id.RoleId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.accesscontrol.api.AccessResolutionFilter;
import com.tchalanet.server.platform.accesscontrol.api.model.request.GetEffectivePermissionsRequest;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.adapter.PermissionCatalogAdminAdapter;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.entity.AppRoleJpaEntity;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.AppRoleJpaRepository;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.TenantUserRoleJpaRepository;
import com.tchalanet.server.platform.accesscontrol.internal.service.EffectivePermissionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccessResolutionFilterImpl extends OncePerRequestFilter implements AccessResolutionFilter {

    static final Set<String> TERMINAL_PERMISSIONS = Set.of(
        "terminal.me.read",
        "terminal.sell",
        "terminal.ticket.read_own",
        "terminal.ticket.reprint_own"
    );

    private final EffectivePermissionService effectivePermissionService;
    private final TenantUserRoleJpaRepository tenantUserRoleRepository;
    private final AppRoleJpaRepository appRoleRepository;
    private final PermissionCatalogAdminAdapter permissionCatalogAdminAdapter;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain chain)
        throws ServletException, IOException {

        var bootstrappedActor = (BootstrappedActor)
            request.getAttribute(TchContextRequestAttributes.BOOTSTRAPPED_ACTOR);

        if (bootstrappedActor == null) {
            // Public endpoint or unauthenticated — no actor to resolve
            chain.doFilter(request, response);
            return;
        }

        ResolvedAccessContext resolved;

        if (bootstrappedActor.isAppUser()) {
            resolved = resolveAppUser(request, response, bootstrappedActor);
            if (resolved == null) return; // 403 already written
        } else if (bootstrappedActor.isSellerTerminal()) {
            resolved = resolveSellerTerminal(bootstrappedActor);
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "unknown.actor_type");
            return;
        }

        request.setAttribute(TchContextRequestAttributes.RESOLVED_ACCESS, resolved);
        enrichSpringAuthentication(resolved);
        chain.doFilter(request, response);
    }

    // package-private for unit testing (avoids @Transactional proxy issue on private)
    ResolvedAccessContext resolveAppUser(
        HttpServletRequest request,
        HttpServletResponse response,
        BootstrappedActor actor)
        throws IOException {

        var userId = actor.appUserId();
        var scope = ApiScopeResolver.resolve(request);

        // Load platform roles (SUPER_ADMIN, etc.) — no RLS, these are platform-scope
        var platformRoleIds = tenantUserRoleRepository
            .findActivePlatformRoleIdsByUser(userId.value())
            .stream().map(RoleId::of).toList();

        var platformRoleCodes = appRoleRepository
            .findAllById(platformRoleIds.stream().map(RoleId::value).toList())
            .stream()
            .filter(r -> r.isSystem() && r.isActive())
            .map(AppRoleJpaEntity::getCode)
            .collect(Collectors.toSet());

        var superAdmin = platformRoleCodes.contains("SUPER_ADMIN");

        // Platform permissions (e.g. platform.tenant.override for SUPER_ADMIN)
        var permissionKeys = new HashSet<String>();
        for (var roleId : platformRoleIds) {
            permissionKeys.addAll(permissionCatalogAdminAdapter.listPermissionCodes(roleId));
        }

        // Resolve tenant ID from request headers
        var overrideHeader = request.getHeader(TchHeaders.X_TCH_TENANT_OVERRIDE);
        var tenantIdHeader = request.getHeader(TchHeaders.X_TENANT_ID);
        var tenantOverride = overrideHeader != null && !overrideHeader.isBlank();
        var rawTenantId = tenantOverride ? overrideHeader : tenantIdHeader;

        TenantId effectiveTenantId = null;
        if (rawTenantId != null && !rawTenantId.isBlank()) {
            try {
                effectiveTenantId = TenantId.of(UUID.fromString(rawTenantId.trim()));
            } catch (IllegalArgumentException ignored) {
                // Malformed UUID → no tenant context; TchContextFilter will reject if required
            }
        }

        var roleCodes = new HashSet<>(platformRoleCodes);

        // Load tenant roles + permissions for TENANT and ADMIN scopes
        if (effectiveTenantId != null && (scope == ApiScope.TENANT || scope == ApiScope.ADMIN)) {
            var effective = effectivePermissionService.getEffectivePermissions(
                new GetEffectivePermissionsRequest(userId, effectiveTenantId));

            if (effective.roleIds().isEmpty()) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "tenant.no_membership");
                return null;
            }

            var tenantRoleCodes = appRoleRepository
                .findAllById(effective.roleIds().stream().map(RoleId::value).toList())
                .stream()
                .filter(AppRoleJpaEntity::isActive)
                .map(AppRoleJpaEntity::getCode)
                .collect(Collectors.toSet());

            roleCodes.addAll(tenantRoleCodes);
            permissionKeys.addAll(effective.permissionCodes());
        }

        return new ResolvedAccessContext(
            TchActorType.APP_USER,
            userId,
            null,
            effectiveTenantId,
            superAdmin,
            tenantOverride,
            roleCodes,
            permissionKeys
        );
    }

    private ResolvedAccessContext resolveSellerTerminal(BootstrappedActor actor) {
        return new ResolvedAccessContext(
            TchActorType.SELLER_TERMINAL,
            null,
            actor.sellerTerminalId(),
            actor.tenantId(),
            false,
            false,
            Set.of(),
            TERMINAL_PERMISSIONS
        );
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

        // DB-owned permission authorities (PERM_terminal.sell, PERM_ticket.void, etc.)
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
