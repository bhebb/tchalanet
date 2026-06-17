package com.tchalanet.server.platform.accesscontrol.internal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.entity.UserPermissionOverrideJpaEntity;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.RoleAccessRow;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.TenantUserRoleJpaRepository;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.UserPermissionOverrideJpaRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccessControlSnapshotResolverTest {

    private static final UserId USER = UserId.of(UUID.randomUUID());
    private static final TenantId TENANT = TenantId.of(UUID.randomUUID());

    @Mock private TenantUserRoleJpaRepository tenantUserRoleRepository;
    @Mock private UserPermissionOverrideJpaRepository overrideRepository;

    private AccessControlSnapshotResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new AccessControlSnapshotResolver(tenantUserRoleRepository, overrideRepository);
    }

    @Test
    void resolvePlatform_collectsRolesAndPermissions_andDetectsSuperAdmin() {
        when(tenantUserRoleRepository.findPlatformRoleAccessRows(USER.value()))
            .thenReturn(List.of(
                row("SUPER_ADMIN", "platform.tenant.override"),
                row("SUPER_ADMIN", "platform.ops.run")));

        var access = resolver.resolvePlatform(USER);

        assertThat(access.superAdmin()).isTrue();
        assertThat(access.roleCodes()).containsExactly("SUPER_ADMIN");
        assertThat(access.permissionKeys())
            .containsExactlyInAnyOrder("platform.tenant.override", "platform.ops.run");
    }

    @Test
    void resolvePlatform_roleWithoutPermissions_yieldsRoleAndNoPermission() {
        when(tenantUserRoleRepository.findPlatformRoleAccessRows(USER.value()))
            .thenReturn(List.of(row("SUPER_ADMIN", null)));

        var access = resolver.resolvePlatform(USER);

        assertThat(access.superAdmin()).isTrue();
        assertThat(access.permissionKeys()).isEmpty();
    }

    @Test
    void resolvePlatform_noPlatformRoles_isNotSuperAdmin() {
        when(tenantUserRoleRepository.findPlatformRoleAccessRows(USER.value()))
            .thenReturn(List.of());

        var access = resolver.resolvePlatform(USER);

        assertThat(access.superAdmin()).isFalse();
        assertThat(access.roleCodes()).isEmpty();
        assertThat(access.permissionKeys()).isEmpty();
    }

    @Test
    void resolveTenant_appliesGrantOverride_andDenyWins() {
        when(tenantUserRoleRepository.findTenantRoleAccessRows(TENANT.value(), USER.value()))
            .thenReturn(List.of(
                row("TENANT_ADMIN", "ticket.read"),
                row("TENANT_ADMIN", "ticket.void")));
        when(overrideRepository.findActiveByTenantAndUser(TENANT.value(), USER.value()))
            .thenReturn(List.of(
                override("GRANT", "report.read"),
                override("DENY", "ticket.void")));

        var access = resolver.resolveTenant(USER, TENANT);

        assertThat(access.roleCodes()).containsExactly("TENANT_ADMIN");
        assertThat(access.permissionKeys())
            .containsExactlyInAnyOrder("ticket.read", "report.read");
    }

    @Test
    void resolveTenant_denyWinsOverGrantOfSamePermission() {
        when(tenantUserRoleRepository.findTenantRoleAccessRows(TENANT.value(), USER.value()))
            .thenReturn(List.of(row("TENANT_ADMIN", null)));
        when(overrideRepository.findActiveByTenantAndUser(TENANT.value(), USER.value()))
            .thenReturn(List.of(override("GRANT", "p"), override("DENY", "p")));

        var access = resolver.resolveTenant(USER, TENANT);

        assertThat(access.permissionKeys()).doesNotContain("p");
    }

    private static RoleAccessRow row(String roleCode, String permissionCode) {
        return new RoleAccessRow() {
            @Override
            public String getRoleCode() {
                return roleCode;
            }

            @Override
            public String getPermissionCode() {
                return permissionCode;
            }
        };
    }

    private static UserPermissionOverrideJpaEntity override(String effect, String permissionCode) {
        var e = new UserPermissionOverrideJpaEntity();
        e.setEffect(effect);
        e.setPermissionCode(permissionCode);
        return e;
    }
}
