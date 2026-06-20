package com.tchalanet.server.platform.accesscontrol.internal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.entity.UserPermissionOverrideJpaEntity;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.PlatformUserRoleJpaRepository;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.RoleAccessRow;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.TenantUserRoleJpaRepository;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.UserAccessRow;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.UserAccessSnapshotJpaRepository;
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

    @Mock private PlatformUserRoleJpaRepository platformUserRoleRepository;
    @Mock private UserAccessSnapshotJpaRepository userAccessSnapshotRepository;
    @Mock private TenantUserRoleJpaRepository tenantUserRoleRepository;
    @Mock private UserPermissionOverrideJpaRepository overrideRepository;

    private AccessControlSnapshotResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new AccessControlSnapshotResolver(
            platformUserRoleRepository, userAccessSnapshotRepository,
            tenantUserRoleRepository, overrideRepository);
    }

    @Test
    void resolvePlatform_collectsRolesAndPermissions_andDetectsSuperAdmin() {
        when(platformUserRoleRepository.findPlatformRoleAccessRows(USER.value()))
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
        when(platformUserRoleRepository.findPlatformRoleAccessRows(USER.value()))
            .thenReturn(List.of(row("SUPER_ADMIN", null)));

        var access = resolver.resolvePlatform(USER);

        assertThat(access.superAdmin()).isTrue();
        assertThat(access.permissionKeys()).isEmpty();
    }

    @Test
    void resolvePlatform_noPlatformRoles_isNotSuperAdmin() {
        when(platformUserRoleRepository.findPlatformRoleAccessRows(USER.value()))
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

    @Test
    void resolveUserAccess_buildsGlobalSnapshotAndAppliesTenantOverrides() {
        when(userAccessSnapshotRepository.findUserAccessRows(USER.value()))
            .thenReturn(List.of(
                accessRow(null, null, null, null, "PLATFORM", "SUPER_ADMIN", "platform.tenant.override"),
                accessRow(TENANT.value(), "demo", "Demo", "ACTIVE", "TENANT", "TENANT_ADMIN", "ticket.read"),
                accessRow(TENANT.value(), "demo", "Demo", "ACTIVE", "TENANT", "TENANT_ADMIN", "ticket.void")));
        when(overrideRepository.findActiveByUser(USER.value()))
            .thenReturn(List.of(
                override(TENANT.value(), "GRANT", "report.read"),
                override(TENANT.value(), "DENY", "ticket.void")));

        var snapshot = resolver.resolveUserAccess(USER);

        assertThat(snapshot.platform().superAdmin()).isTrue();
        assertThat(snapshot.platform().permissionKeys()).containsExactly("platform.tenant.override");
        assertThat(snapshot.tenantScopes()).hasSize(1);
        var tenant = snapshot.tenantScopes().getFirst();
        assertThat(tenant.tenantId()).isEqualTo(TENANT);
        assertThat(tenant.roleCodes()).containsExactly("TENANT_ADMIN");
        assertThat(tenant.permissionKeys()).containsExactlyInAnyOrder("ticket.read", "report.read");
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
        return override(TENANT.value(), effect, permissionCode);
    }

    private static UserPermissionOverrideJpaEntity override(
        UUID tenantId, String effect, String permissionCode) {
        var e = new UserPermissionOverrideJpaEntity();
        e.setTenantId(tenantId);
        e.setEffect(effect);
        e.setPermissionCode(permissionCode);
        return e;
    }

    private static UserAccessRow accessRow(
        UUID tenantId,
        String tenantCode,
        String tenantName,
        String tenantStatus,
        String scope,
        String roleCode,
        String permissionCode) {
        return new UserAccessRow() {
            @Override
            public UUID getUserId() {
                return USER.value();
            }

            @Override
            public UUID getTenantId() {
                return tenantId;
            }

            @Override
            public String getTenantCode() {
                return tenantCode;
            }

            @Override
            public String getTenantName() {
                return tenantName;
            }

            @Override
            public String getTenantStatus() {
                return tenantStatus;
            }

            @Override
            public String getScope() {
                return scope;
            }

            @Override
            public String getRoleCode() {
                return roleCode;
            }

            @Override
            public String getPermissionCode() {
                return permissionCode;
            }

            @Override
            public UUID getSellerTerminalId() {
                return null;
            }

            @Override
            public String getTerminalCode() {
                return null;
            }

            @Override
            public String getSellerTerminalStatus() {
                return null;
            }
        };
    }
}
