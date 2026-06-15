package com.tchalanet.server.platform.accesscontrol.internal.web;

import static com.tchalanet.server.common.context.TchContextRequestAttributes.BOOTSTRAPPED_ACTOR;
import static com.tchalanet.server.common.context.TchContextRequestAttributes.RESOLVED_ACCESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.context.BootstrappedActor;
import com.tchalanet.server.common.context.ResolvedAccessContext;
import com.tchalanet.server.common.context.TchActorType;
import com.tchalanet.server.common.types.id.RoleId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.platform.accesscontrol.api.model.request.GetEffectivePermissionsRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.view.EffectivePermissionsView;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.adapter.PermissionCatalogAdminAdapter;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.entity.AppRoleJpaEntity;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.AppRoleJpaRepository;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.TenantUserRoleJpaRepository;
import com.tchalanet.server.platform.accesscontrol.internal.service.EffectivePermissionService;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class AccessResolutionFilterImplTest {

    private final EffectivePermissionService effectivePermissionService =
        mock(EffectivePermissionService.class);
    private final TenantUserRoleJpaRepository tenantUserRoleRepository =
        mock(TenantUserRoleJpaRepository.class);
    private final AppRoleJpaRepository appRoleRepository = mock(AppRoleJpaRepository.class);
    private final PermissionCatalogAdminAdapter permissionCatalogAdminAdapter =
        mock(PermissionCatalogAdminAdapter.class);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ── pass-through ──────────────────────────────────────────────────────────

    @Test
    void noBootstrappedActor_passesThrough() throws Exception {
        var chain = new MockFilterChain();
        var response = new MockHttpServletResponse();
        filter().doFilter(new MockHttpServletRequest(), response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
        verify(effectivePermissionService, never()).getEffectivePermissions(any());
    }

    // ── APP_USER — tenant scope ───────────────────────────────────────────────

    @Test
    void tenantAdmin_receives_ROLE_TENANT_ADMIN() throws Exception {
        var userId = UserId.of(UUID.randomUUID());
        var tenantId = TenantId.of(UUID.randomUUID());
        var roleId = RoleId.of(UUID.randomUUID());

        setupAuth();
        setupNoPlatformRoles(userId);
        setupTenantMembership(userId, tenantId, roleId, "TENANT_ADMIN", Set.of("ticket.read"));

        var request = tenantRequest("/api/v1/tenant/foo", tenantId);
        request.setAttribute(BOOTSTRAPPED_ACTOR, BootstrappedActor.appUser(
            userId, "FIREBASE", "https://issuer.example", "sub-1"));

        var response = new MockHttpServletResponse();
        filter().doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
        assertAuthority("ROLE_TENANT_ADMIN");
        assertAuthority("ACTOR_APP_USER");
        var resolved = (ResolvedAccessContext) request.getAttribute(RESOLVED_ACCESS);
        assertThat(resolved.roleCodes()).contains("TENANT_ADMIN");
        assertThat(resolved.effectiveTenantId()).isEqualTo(tenantId);
    }

    @Test
    void tenantOwner_receives_ROLE_TENANT_OWNER() throws Exception {
        var userId = UserId.of(UUID.randomUUID());
        var tenantId = TenantId.of(UUID.randomUUID());
        var roleId = RoleId.of(UUID.randomUUID());

        setupAuth();
        setupNoPlatformRoles(userId);
        setupTenantMembership(userId, tenantId, roleId, "TENANT_OWNER", Set.of("tenant.manage"));

        var request = tenantRequest("/api/v1/tenant/me", tenantId);
        request.setAttribute(BOOTSTRAPPED_ACTOR, BootstrappedActor.appUser(
            userId, "KEYCLOAK", "https://issuer.example", "sub-owner"));

        var response = new MockHttpServletResponse();
        filter().doFilter(request, response, new MockFilterChain());

        assertAuthority("ROLE_TENANT_OWNER");
        assertAuthority("ACTOR_APP_USER");
    }

    @Test
    void permissionDeny_removesAuthority() throws Exception {
        var userId = UserId.of(UUID.randomUUID());
        var tenantId = TenantId.of(UUID.randomUUID());
        var roleId = RoleId.of(UUID.randomUUID());

        setupAuth();
        setupNoPlatformRoles(userId);
        // effective permissions already has DENY applied — effectivePermissionService returns final set
        setupTenantMembership(userId, tenantId, roleId, "TENANT_ADMIN", Set.of("ticket.read"));
        // ticket.void was denied — not in permissionCodes

        var request = tenantRequest("/api/v1/tenant/foo", tenantId);
        request.setAttribute(BOOTSTRAPPED_ACTOR, BootstrappedActor.appUser(
            userId, "FIREBASE", "https://issuer.example", "sub-deny"));

        filter().doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        var auth = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        var names = auth.getAuthorities().stream()
            .map(a -> a.getAuthority()).toList();
        assertThat(names).doesNotContain("PERM_ticket.void");
        assertThat(names).contains("PERM_ticket.read");
    }

    @Test
    void userWithoutActiveMembership_refusedOnTenantScope() throws Exception {
        var userId = UserId.of(UUID.randomUUID());
        var tenantId = TenantId.of(UUID.randomUUID());

        setupAuth();
        setupNoPlatformRoles(userId);
        when(effectivePermissionService.getEffectivePermissions(
            new GetEffectivePermissionsRequest(userId, tenantId)))
            .thenReturn(new EffectivePermissionsView(tenantId, userId, List.of(), Set.of()));
        when(appRoleRepository.findAllById(List.of())).thenReturn(List.of());

        var request = tenantRequest("/api/v1/tenant/foo", tenantId);
        request.setAttribute(BOOTSTRAPPED_ACTOR, BootstrappedActor.appUser(
            userId, "FIREBASE", "https://issuer.example", "sub-no-member"));

        var response = new MockHttpServletResponse();
        filter().doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getErrorMessage()).isEqualTo("tenant.no_membership");
    }

    // ── APP_USER — platform scope ─────────────────────────────────────────────

    @Test
    void superAdminPlatform_receives_ROLE_SUPER_ADMIN_withoutTenant() throws Exception {
        var userId = UserId.of(UUID.randomUUID());
        var superAdminRoleId = RoleId.of(UUID.randomUUID());

        setupAuth();
        when(tenantUserRoleRepository.findActivePlatformRoleIdsByUser(userId.value()))
            .thenReturn(List.of(superAdminRoleId.value()));
        var superAdminEntity = roleEntity(superAdminRoleId.value(), "SUPER_ADMIN");
        when(appRoleRepository.findAllById(List.of(superAdminRoleId.value())))
            .thenReturn(List.of(superAdminEntity));
        when(permissionCatalogAdminAdapter.listPermissionCodes(superAdminRoleId))
            .thenReturn(Set.of("platform.tenant.override"));

        var request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/platform/tenants");
        request.setAttribute(BOOTSTRAPPED_ACTOR, BootstrappedActor.appUser(
            userId, "FIREBASE", "https://issuer.example", "sub-sa"));

        var response = new MockHttpServletResponse();
        filter().doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
        assertAuthority("ROLE_SUPER_ADMIN");
        assertAuthority("PERM_platform.tenant.override");
        var resolved = (ResolvedAccessContext) request.getAttribute(RESOLVED_ACCESS);
        assertThat(resolved.superAdmin()).isTrue();
        assertThat(resolved.effectiveTenantId()).isNull();
        // Must NOT call effective permissions for platform scope without tenantId
        verify(effectivePermissionService, never()).getEffectivePermissions(any());
    }

    @Test
    void superAdminOverride_receivesEffectiveTenant_andExpectedRoles() throws Exception {
        var userId = UserId.of(UUID.randomUUID());
        var superAdminRoleId = RoleId.of(UUID.randomUUID());
        var tenantId = TenantId.of(UUID.randomUUID());
        var tenantAdminRoleId = RoleId.of(UUID.randomUUID());

        setupAuth();
        when(tenantUserRoleRepository.findActivePlatformRoleIdsByUser(userId.value()))
            .thenReturn(List.of(superAdminRoleId.value()));
        var superAdminEntity = roleEntity(superAdminRoleId.value(), "SUPER_ADMIN");
        when(appRoleRepository.findAllById(List.of(superAdminRoleId.value())))
            .thenReturn(List.of(superAdminEntity));
        when(permissionCatalogAdminAdapter.listPermissionCodes(superAdminRoleId))
            .thenReturn(Set.of("platform.tenant.override"));

        // Override scope: ADMIN route with X-Tch-Tenant-Override header
        setupTenantMembership(userId, tenantId, tenantAdminRoleId, "TENANT_ADMIN", Set.of("ticket.read"));

        var request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/admin/tenants/" + tenantId.value());
        request.addHeader("X-Tch-Tenant-Override", tenantId.value().toString());
        request.addHeader("X-Tch-Override-Reason", "support ticket #42");
        request.setAttribute(BOOTSTRAPPED_ACTOR, BootstrappedActor.appUser(
            userId, "FIREBASE", "https://issuer.example", "sub-sa"));

        var response = new MockHttpServletResponse();
        filter().doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
        var resolved = (ResolvedAccessContext) request.getAttribute(RESOLVED_ACCESS);
        assertThat(resolved.superAdmin()).isTrue();
        assertThat(resolved.tenantOverride()).isTrue();
        assertThat(resolved.effectiveTenantId()).isEqualTo(tenantId);
        assertThat(resolved.roleCodes()).contains("SUPER_ADMIN", "TENANT_ADMIN");
    }

    // ── SELLER_TERMINAL ────────────────────────────────────────────────────────

    @Test
    void sellerTerminal_receives_ACTOR_SELLER_TERMINAL() throws Exception {
        var terminalId = SellerTerminalId.of(UUID.randomUUID());
        var tenantId = TenantId.of(UUID.randomUUID());

        setupAuth();
        var request = new MockHttpServletRequest();
        request.setAttribute(BOOTSTRAPPED_ACTOR,
            BootstrappedActor.sellerTerminal(terminalId, tenantId, "FIREBASE", "https://iss", "sub-t"));

        var response = new MockHttpServletResponse();
        filter().doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
        assertAuthority("ACTOR_SELLER_TERMINAL");
        var resolved = (ResolvedAccessContext) request.getAttribute(RESOLVED_ACCESS);
        assertThat(resolved.actorType()).isEqualTo(TchActorType.SELLER_TERMINAL);
        assertThat(resolved.sellerTerminalId()).isEqualTo(terminalId);
        assertThat(resolved.effectiveTenantId()).isEqualTo(tenantId);
        // SellerTerminal resolution does not touch DB repositories
        verify(tenantUserRoleRepository, never()).findActivePlatformRoleIdsByUser(any());
        verify(effectivePermissionService, never()).getEffectivePermissions(any());
    }

    @Test
    void activeSellerTerminal_receives_PERM_terminal_sell() throws Exception {
        var terminalId = SellerTerminalId.of(UUID.randomUUID());
        var tenantId = TenantId.of(UUID.randomUUID());

        setupAuth();
        var request = new MockHttpServletRequest();
        request.setAttribute(BOOTSTRAPPED_ACTOR,
            BootstrappedActor.sellerTerminal(terminalId, tenantId, "FIREBASE", "https://iss", "sub-t2"));

        filter().doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertAuthority("PERM_terminal.sell");
        assertAuthority("PERM_terminal.me.read");
        assertAuthority("PERM_terminal.ticket.read_own");
        assertAuthority("PERM_terminal.ticket.reprint_own");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private AccessResolutionFilterImpl filter() {
        return new AccessResolutionFilterImpl(
            effectivePermissionService,
            tenantUserRoleRepository,
            appRoleRepository,
            permissionCatalogAdminAdapter);
    }

    private void setupAuth() {
        var jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("test-subject");
        SecurityContextHolder.getContext()
            .setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    private void setupNoPlatformRoles(UserId userId) {
        when(tenantUserRoleRepository.findActivePlatformRoleIdsByUser(userId.value()))
            .thenReturn(List.of());
        when(appRoleRepository.findAllById(List.of())).thenReturn(List.of());
    }

    private void setupTenantMembership(
        UserId userId, TenantId tenantId, RoleId roleId, String roleCode, Set<String> permissions) {
        when(effectivePermissionService.getEffectivePermissions(
            new GetEffectivePermissionsRequest(userId, tenantId)))
            .thenReturn(new EffectivePermissionsView(tenantId, userId, List.of(roleId), permissions));
        var roleEntity = roleEntity(roleId.value(), roleCode);
        when(appRoleRepository.findAllById(List.of(roleId.value())))
            .thenReturn(List.of(roleEntity));
    }

    private AppRoleJpaEntity roleEntity(UUID id, String code) {
        var entity = new AppRoleJpaEntity();
        entity.setId(id);
        entity.setCode(code);
        entity.setName(code);
        entity.setActive(true);
        entity.setSystem(true);
        return entity;
    }

    private MockHttpServletRequest tenantRequest(String uri, TenantId tenantId) {
        var request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        request.addHeader("X-Tenant-Id", tenantId.value().toString());
        return request;
    }

    private void assertAuthority(String authority) {
        var auth = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getAuthorities())
            .extracting("authority")
            .contains(authority);
    }
}
