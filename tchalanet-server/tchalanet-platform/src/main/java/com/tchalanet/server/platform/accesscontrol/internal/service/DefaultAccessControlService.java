package com.tchalanet.server.platform.accesscontrol.internal.service;

import com.tchalanet.server.common.types.id.RoleId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.accesscontrol.api.AccessControlApi;
import com.tchalanet.server.platform.accesscontrol.api.model.request.AssignRoleToUserRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.BootstrapAccessControlRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.CheckUserPermissionsRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.CreateRoleRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.DenyUserPermissionRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.GetEffectivePermissionsRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.GrantPermissionToRoleRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.GrantUserPermissionRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.ListPermissionsRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.ListRolePermissionsRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.ListRolesRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.RemoveRoleFromUserRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.RemoveUserPermissionOverrideRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.RevokePermissionFromRoleRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.SetTenantUserRoleRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.UpdateRoleRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.result.BootstrapAccessControlResult;
import com.tchalanet.server.platform.accesscontrol.api.model.result.CheckUserPermissionsResult;
import com.tchalanet.server.platform.accesscontrol.api.model.view.EffectivePermissionsView;
import com.tchalanet.server.platform.accesscontrol.api.model.view.PermissionView;
import com.tchalanet.server.platform.accesscontrol.api.model.view.RolePermissionView;
import com.tchalanet.server.platform.accesscontrol.api.model.view.RoleView;
import com.tchalanet.server.platform.accesscontrol.internal.bootstrap.AccessControlBootstrapService;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.adapter.PermissionCatalogAdminAdapter;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.adapter.TenantUserRoleJpaAdapter;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.entity.AppRoleJpaEntity;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.entity.UserPermissionOverrideJpaEntity;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.AppRoleJpaRepository;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.TenantUserRoleJpaRepository;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.UserPermissionOverrideJpaRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultAccessControlService implements AccessControlApi {

  private final AppRoleJpaRepository appRoleRepository;
  private final PermissionCatalogAdminAdapter permissionCatalogAdminAdapter;
  private final TenantUserRoleWriterPort tenantUserRoleWriter;
  private final TenantUserRoleJpaRepository tenantUserRoleRepository;
  private final UserPermissionOverrideJpaRepository overrideRepository;
  private final TenantUserRoleJpaAdapter tenantUserRoleAdapter;
  private final AccessControlBootstrapService bootstrapService;

  // ─── Permission evaluation ────────────────────────────────────────────────

  @Override
  public CheckUserPermissionsResult checkPermissions(CheckUserPermissionsRequest request) {
    Set<String> required = request.requiredPermissions() == null ? Set.of() : request.requiredPermissions();
    if (required.isEmpty()) {
      return new CheckUserPermissionsResult(true, Set.of());
    }
    var effective = getEffectivePermissions(
        new GetEffectivePermissionsRequest(request.userId(), request.tenantId()));
    var missing = required.stream()
        .filter(p -> !effective.permissionCodes().contains(p))
        .collect(Collectors.toSet());
    return new CheckUserPermissionsResult(missing.isEmpty(), missing);
  }

  @Override
  @Transactional(readOnly = true)
  public EffectivePermissionsView getEffectivePermissions(GetEffectivePermissionsRequest request) {
    var tenantId = request.tenantId();
    var userId = request.userId();

    // 1. Collect permissions from all active roles
    var roleIds = tenantUserRoleRepository
        .findActiveByTenantAndUser(tenantId.value(), userId.value())
        .stream()
        .map(r -> RoleId.of(r.getRoleId()))
        .toList();

    Set<String> rolePermissions = new HashSet<>();
    for (var roleId : roleIds) {
      rolePermissions.addAll(permissionCatalogAdminAdapter.listPermissionCodes(roleId));
    }

    // 2. Apply user-level overrides: GRANT adds, DENY removes (DENY wins)
    var overrides = overrideRepository
        .findActiveByTenantAndUser(tenantId.value(), userId.value());

    Set<String> grants = overrides.stream()
        .filter(o -> "GRANT".equals(o.getEffect()))
        .map(UserPermissionOverrideJpaEntity::getPermissionCode)
        .collect(Collectors.toSet());

    Set<String> denies = overrides.stream()
        .filter(o -> "DENY".equals(o.getEffect()))
        .map(UserPermissionOverrideJpaEntity::getPermissionCode)
        .collect(Collectors.toSet());

    Set<String> effective = new HashSet<>(rolePermissions);
    effective.addAll(grants);
    effective.removeAll(denies); // DENY wins over role grants and explicit GRANTs

    return new EffectivePermissionsView(tenantId, userId, roleIds, Set.copyOf(effective));
  }

  // ─── Catalog reads ────────────────────────────────────────────────────────

  @Override
  public List<RoleView> listRoles(ListRolesRequest request) {
    List<AppRoleJpaEntity> roles =
        request.tenantId() == null
            ? appRoleRepository.findAllGlobalNotDeleted()
            : appRoleRepository.findAllForTenantOrGlobal(request.tenantId().value());
    return roles.stream().map(this::toRoleView).toList();
  }

  @Override
  public List<PermissionView> listPermissions(ListPermissionsRequest request) {
    return permissionCatalogAdminAdapter.listPermissions().stream()
        .map(s -> new PermissionView(s.code(), s.name(), s.category(), s.description()))
        .toList();
  }

  @Override
  public List<RolePermissionView> listRolePermissions(ListRolePermissionsRequest request) {
    return permissionCatalogAdminAdapter.listPermissionCodes(request.roleId()).stream()
        .map(code -> new RolePermissionView(request.roleId(), code))
        .toList();
  }

  // ─── Role assignment ──────────────────────────────────────────────────────

  @Override
  @Transactional
  public void assignRoleToUser(AssignRoleToUserRequest request) {
    tenantUserRoleAdapter.assign(request.tenantId(), request.userId(), request.roleCode(), request.assignedBy());
    log.info("Assigned role {} to user {} in tenant {}", request.roleCode(), request.userId(), request.tenantId());
  }

  @Override
  @Transactional
  public void removeRoleFromUser(RemoveRoleFromUserRequest request) {
    tenantUserRoleAdapter.remove(request.tenantId(), request.userId(), request.roleCode());
    log.info("Removed role {} from user {} in tenant {}", request.roleCode(), request.userId(), request.tenantId());
  }

  // ─── User permission overrides ────────────────────────────────────────────

  @Override
  @Transactional
  public void grantUserPermission(GrantUserPermissionRequest request) {
    upsertOverride(request.tenantId(), request.userId().value(), request.permissionCode(),
        "GRANT", request.reason(), request.grantedBy() == null ? null : request.grantedBy().value());
  }

  @Override
  @Transactional
  public void denyUserPermission(DenyUserPermissionRequest request) {
    upsertOverride(request.tenantId(), request.userId().value(), request.permissionCode(),
        "DENY", request.reason(), request.deniedBy() == null ? null : request.deniedBy().value());
  }

  @Override
  @Transactional
  public void removeUserPermissionOverride(RemoveUserPermissionOverrideRequest request) {
    int removed = overrideRepository.softDelete(
        request.tenantId().value(), request.userId().value(), request.permissionCode());
    log.info("Removed {} permission override(s) for {} on {}", removed, request.userId(), request.permissionCode());
  }

  private void upsertOverride(TenantId tenantId, UUID userId, String code, String effect, String reason, UUID actorId) {
    // Soft-delete existing active override first (unique active constraint)
    overrideRepository.softDelete(tenantId.value(), userId, code);
    var entity = new UserPermissionOverrideJpaEntity();
    entity.setTenantId(tenantId.value());
    entity.setUserId(userId);
    entity.setPermissionCode(code);
    entity.setEffect(effect);
    entity.setReason(reason);
    entity.setCreatedBy(actorId);
    overrideRepository.save(entity);
    log.info("User {} permission override: {} {} by {}", userId, effect, code, actorId);
  }

  // ─── Bootstrap ───────────────────────────────────────────────────────────

  @Override
  public BootstrapAccessControlResult bootstrap(BootstrapAccessControlRequest request) {
    return bootstrapService.execute(request);
  }

  // ─── Legacy role catalog management ──────────────────────────────────────

  @Override
  public RoleView createRole(CreateRoleRequest request) {
    var entity = new AppRoleJpaEntity();
    entity.setCode(request.code());
    entity.setName(request.name());
    entity.setDescription(request.description());
    entity.setTenantId(request.tenantId() == null ? null : request.tenantId().value());
    UUID roleUuid = appRoleRepository.save(entity).getId();
    return appRoleRepository.findById(roleUuid).map(this::toRoleView)
        .orElseGet(() -> new RoleView(RoleId.of(roleUuid), request.code(), request.name(), request.description(), request.tenantId(), null, request.system()));
  }

  @Override
  public RoleView updateRole(UpdateRoleRequest request) {
    var entity = appRoleRepository.findById(request.id().value())
        .orElseThrow(() -> new IllegalArgumentException("Role not found: " + request.id()));
    entity.setCode(request.code());
    entity.setName(request.name());
    entity.setDescription(request.description());
    entity.setTenantId(request.tenantId() == null ? null : request.tenantId().value());
    UUID roleUuid = appRoleRepository.save(entity).getId();
    return appRoleRepository.findById(roleUuid).map(this::toRoleView)
        .orElseGet(() -> new RoleView(RoleId.of(roleUuid), request.code(), request.name(), request.description(), request.tenantId(), null, request.system()));
  }

  @Override
  public void grantPermission(GrantPermissionToRoleRequest request) {
    permissionCatalogAdminAdapter.grant(request.roleId(), request.permissionCode());
  }

  @Override
  public void revokePermission(RevokePermissionFromRoleRequest request) {
    permissionCatalogAdminAdapter.revoke(request.roleId(), request.permissionCode());
  }

  @Override
  @Deprecated
  public void setTenantUserRole(SetTenantUserRoleRequest request) {
    var roleEntity = appRoleRepository.findByCode(request.role().name())
        .orElseThrow(() -> new IllegalArgumentException("Role not found: " + request.role()));
    tenantUserRoleWriter.setUserRole(request.tenantId(), request.userId(), RoleId.of(roleEntity.getId()));
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────

  private RoleView toRoleView(AppRoleJpaEntity entity) {
    return new RoleView(
        RoleId.of(entity.getId()),
        entity.getCode(),
        entity.getName(),
        entity.getDescription(),
        TenantId.nullableOf(entity.getTenantId()),
        null,
        entity.getTenantId() == null);
  }
}
