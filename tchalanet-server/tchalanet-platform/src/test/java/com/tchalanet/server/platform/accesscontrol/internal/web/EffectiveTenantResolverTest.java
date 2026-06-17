package com.tchalanet.server.platform.accesscontrol.internal.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.http.TchHeaders;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.TenantUserRoleJpaRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class EffectiveTenantResolverTest {

    private static final UserId USER = UserId.of(UUID.randomUUID());
    private static final UUID TENANT = UUID.randomUUID();
    private static final Set<String> WITH_OVERRIDE_PERM = Set.of("platform.tenant.override");

    @Mock private TenantUserRoleJpaRepository tenantUserRoleRepository;

    private EffectiveTenantResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new EffectiveTenantResolver(tenantUserRoleRepository);
    }

    // ── normal user: tenant comes from membership, never from a header ──────────

    @Test
    void normalUser_singleMembership_resolvesIt() {
        when(tenantUserRoleRepository.findDistinctActiveTenantIdsByUser(USER.value()))
            .thenReturn(List.of(TENANT));

        var result = resolver.resolveForAppUser(new MockHttpServletRequest(), USER, false, Set.of());

        assertThat(result.tenantId()).isEqualTo(TenantId.of(TENANT));
        assertThat(result.tenantOverride()).isFalse();
    }

    @Test
    void normalUser_tenantHeaderIsIgnored_membershipWins() {
        when(tenantUserRoleRepository.findDistinctActiveTenantIdsByUser(USER.value()))
            .thenReturn(List.of(TENANT));

        // A normal user passing X-Tenant-Id must not influence the effective tenant.
        var req = new MockHttpServletRequest();
        req.addHeader(TchHeaders.X_TENANT_ID, UUID.randomUUID().toString());

        var result = resolver.resolveForAppUser(req, USER, false, Set.of());

        assertThat(result.tenantId()).isEqualTo(TenantId.of(TENANT));
        assertThat(result.tenantOverride()).isFalse();
    }

    @Test
    void normalUser_multipleMemberships_isAmbiguous() {
        when(tenantUserRoleRepository.findDistinctActiveTenantIdsByUser(USER.value()))
            .thenReturn(List.of(TENANT, UUID.randomUUID()));

        assertThatThrownBy(() ->
            resolver.resolveForAppUser(new MockHttpServletRequest(), USER, false, Set.of()))
            .isInstanceOf(ProblemRestException.class)
            .hasMessage("tenant.ambiguous_membership");
    }

    @Test
    void normalUser_noMembership_resolvesNone() {
        when(tenantUserRoleRepository.findDistinctActiveTenantIdsByUser(USER.value()))
            .thenReturn(List.of());

        var result = resolver.resolveForAppUser(new MockHttpServletRequest(), USER, false, Set.of());

        assertThat(result.tenantId()).isNull();
        assertThat(result.tenantOverride()).isFalse();
    }

    // ── super admin: no tenant by default, tenant only via explicit override ─────

    @Test
    void superAdmin_noOverride_resolvesNone() {
        var result = resolver.resolveForAppUser(new MockHttpServletRequest(), USER, true, Set.of());

        assertThat(result.tenantId()).isNull();
        assertThat(result.tenantOverride()).isFalse();
    }

    @Test
    void superAdmin_validOverride_setsTenantAndOverrideFlag() {
        var req = new MockHttpServletRequest();
        req.addHeader(TchHeaders.X_TCH_TENANT_OVERRIDE, TENANT.toString());
        req.addHeader(TchHeaders.X_TCH_OVERRIDE_REASON, "incident #42");

        var result = resolver.resolveForAppUser(req, USER, true, WITH_OVERRIDE_PERM);

        assertThat(result.tenantId()).isEqualTo(TenantId.of(TENANT));
        assertThat(result.tenantOverride()).isTrue();
    }

    @Test
    void override_withoutPermission_isDenied() {
        var req = new MockHttpServletRequest();
        req.addHeader(TchHeaders.X_TCH_TENANT_OVERRIDE, TENANT.toString());
        req.addHeader(TchHeaders.X_TCH_OVERRIDE_REASON, "reason");

        assertThatThrownBy(() -> resolver.resolveForAppUser(req, USER, true, Set.of()))
            .isInstanceOf(ProblemRestException.class)
            .hasMessage("tenant.override_forbidden");
    }

    @Test
    void override_withoutReason_isDenied() {
        var req = new MockHttpServletRequest();
        req.addHeader(TchHeaders.X_TCH_TENANT_OVERRIDE, TENANT.toString());

        assertThatThrownBy(() -> resolver.resolveForAppUser(req, USER, true, WITH_OVERRIDE_PERM))
            .isInstanceOf(ProblemRestException.class)
            .hasMessage("tenant.override_reason_required");
    }

    @Test
    void override_byNonSuperAdmin_isDenied() {
        var req = new MockHttpServletRequest();
        req.addHeader(TchHeaders.X_TCH_TENANT_OVERRIDE, TENANT.toString());
        req.addHeader(TchHeaders.X_TCH_OVERRIDE_REASON, "reason");

        assertThatThrownBy(() -> resolver.resolveForAppUser(req, USER, false, WITH_OVERRIDE_PERM))
            .isInstanceOf(ProblemRestException.class)
            .hasMessage("tenant.override_not_super_admin");
    }

    @Test
    void override_withInvalidUuid_isDenied() {
        var req = new MockHttpServletRequest();
        req.addHeader(TchHeaders.X_TCH_TENANT_OVERRIDE, "not-a-uuid");
        req.addHeader(TchHeaders.X_TCH_OVERRIDE_REASON, "reason");

        assertThatThrownBy(() -> resolver.resolveForAppUser(req, USER, true, WITH_OVERRIDE_PERM))
            .isInstanceOf(ProblemRestException.class)
            .hasMessage("tenant.override_invalid");
    }
}
