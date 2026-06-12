package com.tchalanet.server.platform.accesscontrol.internal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.scope.ApiScope;
import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.RoleId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.platform.accesscontrol.api.model.request.GetEffectivePermissionsRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.view.EffectivePermissionsView;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.entity.AppRoleJpaEntity;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.adapter.PermissionCatalogAdminAdapter;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.AppRoleJpaRepository;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.TenantUserRoleJpaRepository;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DatabaseActorAuthorizationContextResolverTest {

  @Test
  void replacesUntrustedTokenHintsWithDatabaseAuthorization() {
    var tenantId = TenantId.of(UUID.randomUUID());
    var userId = UserId.of(UUID.randomUUID());
    var roleId = RoleId.of(UUID.randomUUID());
    var permissions = Set.of("ticket.sell");
    var effectivePermissionService = mock(EffectivePermissionService.class);
    var roleRepository = mock(AppRoleJpaRepository.class);
    var tenantUserRoleRepository = mock(TenantUserRoleJpaRepository.class);
    var permissionCatalog = mock(PermissionCatalogAdminAdapter.class);
    when(effectivePermissionService.getEffectivePermissions(
            new GetEffectivePermissionsRequest(userId, tenantId)))
        .thenReturn(new EffectivePermissionsView(tenantId, userId, List.of(roleId), permissions));
    when(roleRepository.findAllById(List.of(roleId.value())))
        .thenReturn(List.of(systemRole(roleId.value(), "CASHIER")));

    var resolved =
        new DatabaseActorAuthorizationContextResolver(
                effectivePermissionService,
                roleRepository,
                tenantUserRoleRepository,
                permissionCatalog)
            .resolve(context(tenantId, userId, Set.of(TchRole.SUPER_ADMIN), Set.of("tenant.override")));

    assertThat(resolved.systemRoles()).containsExactly(TchRole.CASHIER);
    assertThat(resolved.customRoles()).containsExactly("ticket.sell");
    assertThat(resolved.isSuperAdmin()).isFalse();
  }

  @Test
  void confirmsPlatformSystemRoleWithoutTenantContext() {
    var userId = UserId.of(UUID.randomUUID());
    var effectivePermissionService = mock(EffectivePermissionService.class);
    var roleRepository = mock(AppRoleJpaRepository.class);
    var tenantUserRoleRepository = mock(TenantUserRoleJpaRepository.class);
    var permissionCatalog = mock(PermissionCatalogAdminAdapter.class);
    var platformRoleId = RoleId.of(UUID.randomUUID());
    when(tenantUserRoleRepository.findActivePlatformRoleIdsByUser(userId.value()))
        .thenReturn(List.of(platformRoleId.value()));
    when(roleRepository.findAllById(List.of(platformRoleId.value())))
        .thenReturn(List.of(systemRole(platformRoleId.value(), "SUPER_ADMIN")));
    when(permissionCatalog.listPermissionCodes(platformRoleId))
        .thenReturn(Set.of("tenant.override"));

    var resolved =
        new DatabaseActorAuthorizationContextResolver(
                effectivePermissionService,
                roleRepository,
                tenantUserRoleRepository,
                permissionCatalog)
            .resolve(platformContext(userId));

    assertThat(resolved.systemRoles()).containsExactly(TchRole.SUPER_ADMIN);
    assertThat(resolved.customRoles()).containsExactly("tenant.override");
  }

  @Test
  void preservesConfirmedPlatformRoleDuringTenantOverride() {
    var tenantId = TenantId.of(UUID.randomUUID());
    var userId = UserId.of(UUID.randomUUID());
    var platformRoleId = RoleId.of(UUID.randomUUID());
    var effectivePermissionService = mock(EffectivePermissionService.class);
    var roleRepository = mock(AppRoleJpaRepository.class);
    var tenantUserRoleRepository = mock(TenantUserRoleJpaRepository.class);
    var permissionCatalog = mock(PermissionCatalogAdminAdapter.class);
    when(tenantUserRoleRepository.findActivePlatformRoleIdsByUser(userId.value()))
        .thenReturn(List.of(platformRoleId.value()));
    when(permissionCatalog.listPermissionCodes(platformRoleId))
        .thenReturn(Set.of("tenant.override"));
    when(effectivePermissionService.getEffectivePermissions(
            new GetEffectivePermissionsRequest(userId, tenantId)))
        .thenReturn(new EffectivePermissionsView(tenantId, userId, List.of(), Set.of()));
    when(roleRepository.findAllById(List.of(platformRoleId.value())))
        .thenReturn(List.of(systemRole(platformRoleId.value(), "SUPER_ADMIN")));

    var resolved =
        new DatabaseActorAuthorizationContextResolver(
                effectivePermissionService,
                roleRepository,
                tenantUserRoleRepository,
                permissionCatalog)
            .resolve(context(tenantId, userId, Set.of(TchRole.SUPER_ADMIN), Set.of()));

    assertThat(resolved.isSuperAdmin()).isTrue();
    assertThat(resolved.hasPermissionClaim("tenant.override")).isTrue();
  }

  private static AppRoleJpaEntity systemRole(UUID id, String code) {
    var role = new AppRoleJpaEntity();
    role.setId(id);
    role.setCode(code);
    role.setSystem(true);
    role.setActive(true);
    return role;
  }

  private static TchRequestContext context(
      TenantId tenantId, UserId userId, Set<TchRole> tokenRoles, Set<String> tokenPermissions) {
    return new TchRequestContext(
        tenantId.toString(),
        tenantId.value(),
        tenantId.toString(),
        tenantId.value(),
        "external-subject",
        userId.value(),
        tokenRoles,
        tokenPermissions,
        Locale.CANADA_FRENCH,
        "request-id",
        "127.0.0.1",
        "test",
        true,
        "test override",
        "active",
        ApiScope.ADMIN,
        null,
        tenantId,
        ZoneId.of("America/Port-au-Prince"),
        Currency.getInstance("HTG"),
        null);
  }

  private static TchRequestContext platformContext(UserId userId) {
    return new TchRequestContext(
        null,
        null,
        null,
        null,
        "external-subject",
        userId.value(),
        Set.of(TchRole.SUPER_ADMIN),
        Set.of(),
        Locale.CANADA_FRENCH,
        "request-id",
        "127.0.0.1",
        "test",
        false,
        null,
        "active",
        ApiScope.PLATFORM,
        null,
        null,
        ZoneId.of("America/Port-au-Prince"),
        Currency.getInstance("HTG"),
        null);
  }
}
