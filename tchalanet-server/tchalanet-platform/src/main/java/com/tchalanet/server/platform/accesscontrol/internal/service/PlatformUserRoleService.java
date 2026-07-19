package com.tchalanet.server.platform.accesscontrol.internal.service;

import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.platform.accesscontrol.api.PlatformUserRoleApi;
import com.tchalanet.server.platform.accesscontrol.api.error.AccessControlErrorCodes;
import com.tchalanet.server.platform.accesscontrol.api.model.PlatformSuperAdminAccessRow;
import com.tchalanet.server.platform.accesscontrol.api.model.TenantAdminGlobalAccessRow;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.entity.PlatformUserRoleJpaEntity;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.AppRoleJpaRepository;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.PlatformSuperAdminRow;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.PlatformUserRoleJpaRepository;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.TenantAdminGlobalRow;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.TenantUserRoleJpaRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlatformUserRoleService implements PlatformUserRoleApi {

  private static final String PLATFORM_ROLE_SCOPE = "PLATFORM";
  private static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";
  private static final Set<String> ALLOWED_SORT_FIELDS =
      Set.of("displayName", "status", "createdAt");

  private final AppRoleJpaRepository appRoleRepository;
  private final PlatformUserRoleJpaRepository platformUserRoleRepository;
  private final TenantUserRoleJpaRepository tenantUserRoleRepository;

  @Transactional(readOnly = true)
  @Override
  public List<PlatformSuperAdminAccessRow> listSuperAdmins() {
    return platformUserRoleRepository.listActiveSuperAdmins().stream()
        .map(PlatformUserRoleService::toPlatformSuperAdminAccessRow)
        .toList();
  }

  @Transactional(readOnly = true)
  @Override
  public List<UUID> listActiveSuperAdminUserIds() {
    return platformUserRoleRepository.listActiveSuperAdmins().stream()
        .filter(row -> "ACTIVE".equals(row.getStatus()))
        .map(PlatformSuperAdminRow::getUserId)
        .toList();
  }

  /**
   * Global search of TENANT_ADMIN users across all tenants (SUPER_ADMIN use only).
   *
   * @param q optional name/email filter (case-insensitive LIKE)
   * @param page zero-based page index
   * @param size page size
   */
  @Transactional(readOnly = true)
  @Override
  public List<TenantAdminGlobalAccessRow> searchTenantAdmins(
      String q, int page, int size, String sort) {
    String nameLike = (q == null || q.isBlank()) ? null : "%" + q.trim().toLowerCase() + "%";
    int offset = page * size;
    String field = "displayName";
    String dir = "asc";
    if (sort != null && sort.contains(",")) {
      var parts = sort.split(",", 2);
      if (ALLOWED_SORT_FIELDS.contains(parts[0].trim())) field = parts[0].trim();
      if ("desc".equalsIgnoreCase(parts[1].trim())) dir = "desc";
    }
    return tenantUserRoleRepository.searchTenantAdmins(nameLike, field, dir, size, offset).stream()
        .map(PlatformUserRoleService::toTenantAdminGlobalAccessRow)
        .toList();
  }

  @Transactional(readOnly = true)
  @Override
  public Optional<TenantAdminGlobalAccessRow> findTenantAdmin(UserId userId) {
    Objects.requireNonNull(userId, "userId");
    return tenantUserRoleRepository
        .findTenantAdminByUserId(userId.value())
        .map(PlatformUserRoleService::toTenantAdminGlobalAccessRow);
  }

  @Transactional(readOnly = true)
  @Override
  public long countTenantAdmins(String q) {
    String nameLike = (q == null || q.isBlank()) ? null : "%" + q.trim().toLowerCase() + "%";
    return tenantUserRoleRepository.countTenantAdmins(nameLike);
  }

  @Transactional(readOnly = true)
  @Override
  public boolean hasPlatformRole(UserId userId) {
    return userId != null
        && !platformUserRoleRepository.findPlatformRoleAccessRows(userId.value()).isEmpty();
  }

  @Transactional
  @Override
  public void assignSuperAdmin(UserId userId, UserId assignedBy) {
    var role =
        appRoleRepository
            .findActiveSystemRoleByCodeAndScope(SUPER_ADMIN_ROLE, PLATFORM_ROLE_SCOPE)
            .orElseThrow(() -> ProblemRest.of(AccessControlErrorCodes.PLATFORM_ROLE_UNAVAILABLE));
    if (platformUserRoleRepository.findActiveAssignment(userId.value(), role.getId()).isPresent()) {
      return;
    }
    var entity = new PlatformUserRoleJpaEntity();
    entity.setUserId(userId.value());
    entity.setRoleId(role.getId());
    entity.setAssignedBy(assignedBy == null ? null : assignedBy.value());
    platformUserRoleRepository.save(entity);
  }

  @Transactional
  @Override
  public void removeSuperAdmin(UserId userId) {
    var role =
        appRoleRepository
            .findActiveSystemRoleByCodeAndScope(SUPER_ADMIN_ROLE, PLATFORM_ROLE_SCOPE)
            .orElseThrow(() -> ProblemRest.of(AccessControlErrorCodes.PLATFORM_ROLE_UNAVAILABLE));
    platformUserRoleRepository.softDeleteAssignment(userId.value(), role.getId());
  }

  private static PlatformSuperAdminAccessRow toPlatformSuperAdminAccessRow(
      PlatformSuperAdminRow row) {
    return new PlatformSuperAdminAccessRow(
        row.getUserId(),
        row.getEmail(),
        row.getDisplayName(),
        row.getStatus(),
        row.getAssignedAt());
  }

  private static TenantAdminGlobalAccessRow toTenantAdminGlobalAccessRow(TenantAdminGlobalRow row) {
    return new TenantAdminGlobalAccessRow(
        row.getUserId(),
        row.getEmail(),
        row.getDisplayName(),
        row.getStatus(),
        row.getTenantId(),
        row.getTenantName(),
        row.getTenantCode(),
        row.getAssignedAt());
  }
}
