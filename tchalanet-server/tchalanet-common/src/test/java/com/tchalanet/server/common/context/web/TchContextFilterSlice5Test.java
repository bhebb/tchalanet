package com.tchalanet.server.common.context.web;

import com.tchalanet.server.common.context.ResolvedAccessContext;
import com.tchalanet.server.common.context.TchActorType;
import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.common.context.TchContextBinder;
import com.tchalanet.server.common.context.TchContextProperties;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.auth.ActorContextResolver;
import com.tchalanet.server.common.context.operational.OperationalContextResolver;
import com.tchalanet.server.common.context.tenant.TenantContextInfo;
import com.tchalanet.server.common.context.tenant.TenantContextLookup;
import com.tchalanet.server.common.context.tenant.TenantContextResolver;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.ZoneId;
import java.util.Currency;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.tchalanet.server.common.context.TchContextRequestAttributes.RESOLVED_ACCESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TchContextFilterSlice5Test {

    private static final TenantId TENANT_ID = TenantId.of(UUID.randomUUID());

    private final TenantContextLookup tenantLookup = mock(TenantContextLookup.class);
    private final TenantContextResolver tenantContextResolver = new TenantContextResolver(tenantLookup);
    private final ActorContextResolver actorContextResolver = new ActorContextResolver();
    private final TchContextBinder contextBinder = new TchContextBinder();

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        TchContext.clear();
    }

    // ── §8.3 context cleared after request ───────────────────────────────────

    @Test
    void contextCleared_afterRequest() throws Exception {
        setupTenantLookup(TENANT_ID);

        var userId = UserId.of(UUID.randomUUID());
        var resolvedAccess = appUserAccess(userId, TENANT_ID, Set.of("TENANT_ADMIN"), Set.of());

        var request = tenantRequest(TENANT_ID);
        request.setAttribute(RESOLVED_ACCESS, resolvedAccess);

        filter().doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        // ThreadLocal context must be cleared after filter completes
        assertThat(TchContext.currentOrNull()).isNull();
    }

    // ── §8.3 tenant/admin without effective tenant blocks ────────────────────

    @Test
    void tenantScopeWithoutTenant_blocks() throws Exception {
        // No RESOLVED_ACCESS, no X-Tenant-Id header
        var request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/tenant/something");

        var response = new MockHttpServletResponse();
        filter().doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getErrorMessage()).isEqualTo("Tenant required");
    }

    // ── §8.3 platform super admin without override can have tenant null ───────

    @Test
    void superAdminPlatform_noTenantOverride_tenantNull() throws Exception {
        var userId = UserId.of(UUID.randomUUID());
        var resolvedAccess = new ResolvedAccessContext(
            TchActorType.APP_USER, userId, null, null, true, false,
            Set.of("SUPER_ADMIN"), Set.of("platform.tenant.override")
        );

        var request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/platform/tenants");
        request.setAttribute(RESOLVED_ACCESS, resolvedAccess);

        final TchRequestContext[] capturedCtx = {null};
        filter().doFilter(request, new MockHttpServletResponse(), captureChain(capturedCtx));

        assertThat(capturedCtx[0]).isNotNull();
        assertThat(capturedCtx[0].actorType()).isEqualTo(TchActorType.APP_USER);
        assertThat(capturedCtx[0].roleCodes()).contains("SUPER_ADMIN");
        assertThat(capturedCtx[0].tenantIdSafe()).isNull();
    }

    // ── §8.3 SellerTerminal context has actorType and terminal id ─────────────

    @Test
    void sellerTerminal_contextHasActorTypeAndTerminalId() throws Exception {
        setupTenantLookup(TENANT_ID);

        var terminalId = SellerTerminalId.of(UUID.randomUUID());
        var resolvedAccess = new ResolvedAccessContext(
            TchActorType.SELLER_TERMINAL, null, terminalId, TENANT_ID,
            false, false, Set.of(), Set.of("terminal.sell")
        );

        var request = tenantRequest(TENANT_ID);
        request.setAttribute(RESOLVED_ACCESS, resolvedAccess);

        final TchRequestContext[] capturedCtx = {null};
        filter().doFilter(request, new MockHttpServletResponse(), captureChain(capturedCtx));

        assertThat(capturedCtx[0]).isNotNull();
        assertThat(capturedCtx[0].actorType()).isEqualTo(TchActorType.SELLER_TERMINAL);
        assertThat(capturedCtx[0].sellerTerminalId()).isEqualTo(terminalId);
        assertThat(capturedCtx[0].permissionKeys()).contains("terminal.sell");
        assertThat(capturedCtx[0].appUserId()).isNull();
    }

    // ── §8.3 RLS receives expected tenant ─────────────────────────────────────

    @Test
    void rlsTenant_matchesResolvedAccessTenant() throws Exception {
        setupTenantLookup(TENANT_ID);

        var userId = UserId.of(UUID.randomUUID());
        var resolvedAccess = appUserAccess(userId, TENANT_ID, Set.of("TENANT_ADMIN"), Set.of());

        var request = tenantRequest(TENANT_ID);
        request.setAttribute(RESOLVED_ACCESS, resolvedAccess);

        final TchRequestContext[] capturedCtx = {null};
        filter().doFilter(request, new MockHttpServletResponse(), captureChain(capturedCtx));

        assertThat(capturedCtx[0]).isNotNull();
        assertThat(capturedCtx[0].tenantIdSafe()).isEqualTo(TENANT_ID);
    }

    // ── §8.3 resolved roles and permissions flow into TchRequestContext ───────

    @Test
    void resolvedAccess_setsRolesPermissionsAndAppUserId() throws Exception {
        setupTenantLookup(TENANT_ID);

        var userId = UserId.of(UUID.randomUUID());
        var resolvedAccess = appUserAccess(userId, TENANT_ID, Set.of("TENANT_ADMIN"), Set.of("ticket.read"));

        var request = tenantRequest(TENANT_ID);
        request.setAttribute(RESOLVED_ACCESS, resolvedAccess);

        final TchRequestContext[] capturedCtx = {null};
        filter().doFilter(request, new MockHttpServletResponse(), captureChain(capturedCtx));

        assertThat(capturedCtx[0]).isNotNull();
        assertThat(capturedCtx[0].actorType()).isEqualTo(TchActorType.APP_USER);
        assertThat(capturedCtx[0].roleCodes()).contains("TENANT_ADMIN");
        assertThat(capturedCtx[0].permissionKeys()).contains("ticket.read");
        assertThat(capturedCtx[0].appUserId()).isEqualTo(userId.value());
    }

    // ── §8.3 unauthenticated tenant request is deny-safe ──────────────────────

    @Test
    void unauthenticatedTenantRequest_withSensitiveHeader_isBlocked() throws Exception {
        // No RESOLVED_ACCESS: legacy path. A sensitive header (X-Deleted-Visibility) cannot grant
        // elevation — the deleted-visibility hint is only honored for resolved SUPER_ADMIN actors,
        // and a tenant-scoped request with no resolvable tenant is denied before the controller.
        var request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/tenant/something");
        request.addHeader("X-Deleted-Visibility", "all");

        var response = new MockHttpServletResponse();
        filter().doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getErrorMessage()).isEqualTo("Tenant required");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private TchContextFilter filter() {
        @SuppressWarnings("unchecked")
        ObjectProvider<OperationalContextResolver> operationalProvider = mock(ObjectProvider.class);
        when(operationalProvider.getIfAvailable()).thenReturn(null);

        var props = mock(TchContextProperties.class);
        when(props.publicDefaultTenantCode()).thenReturn(null);

        var factory = new TchRequestContextFactory();

        return new TchContextFilter(
            props, tenantContextResolver, actorContextResolver, factory, contextBinder,
            operationalProvider);
    }

    private void setupTenantLookup(TenantId tenantId) {
        var info = new TenantContextInfo(
            tenantId, "test-tenant", Currency.getInstance("HTG"), ZoneId.of("UTC"));
        when(tenantLookup.findById(tenantId)).thenReturn(Optional.of(info));
        when(tenantLookup.findByCode(any())).thenReturn(Optional.empty());
    }

    private MockHttpServletRequest tenantRequest(TenantId tenantId) {
        var request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/tenant/something");
        request.addHeader("X-Tenant-Id", tenantId.value().toString());
        return request;
    }

    private ResolvedAccessContext appUserAccess(
        UserId userId, TenantId tenantId, Set<String> roles, Set<String> permissions) {
        return new ResolvedAccessContext(
            TchActorType.APP_USER, userId, null, tenantId,
            false, false, roles, permissions
        );
    }

    private MockFilterChain captureChain(TchRequestContext[] slot) {
        return new MockFilterChain() {
            @Override
            public void doFilter(
                jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res)
                throws java.io.IOException, jakarta.servlet.ServletException {
                slot[0] = TchContext.currentOrNull();
                super.doFilter(req, res);
            }
        };
    }
}
