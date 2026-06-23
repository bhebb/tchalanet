package com.tchalanet.server.platform.accesscontrol.internal.service;

import com.tchalanet.server.common.types.id.UserId;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlatformUserRoleService {

  private static final String PLATFORM_ROLE_SCOPE = "PLATFORM";
  private static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";
  private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("displayName", "status", "createdAt");

  private final AppRoleJpaRepository appRoleRepository;
  private final PlatformUserRoleJpaRepository platformUserRoleRepository;
  private final TenantUserRoleJpaRepository tenantUserRoleRepository;

  @Transactional(readOnly = true)
  public List<PlatformSuperAdminRow> listSuperAdmins() {
    return platformUserRoleRepository.listActiveSuperAdmins();
  }

  /**
   * Global search of TENANT_ADMIN users across all tenants (SUPER_ADMIN use only).
   * @param q optional name/email filter (case-insensitive LIKE)
   * @param page zero-based page index
   * @param size page size
   */
  @Transactional(readOnly = true)
  public List<TenantAdminGlobalRow> searchTenantAdmins(String q, int page, int size, String sort) {
    String nameLike = (q == null || q.isBlank()) ? null : "%" + q.trim().toLowerCase() + "%";
    int offset = page * size;
    String field = "displayName";
    String dir   = "asc";
    if (sort != null && sort.contains(",")) {
      var parts = sort.split(",", 2);
      if (ALLOWED_SORT_FIELDS.contains(parts[0].trim())) field = parts[0].trim();
      if ("desc".equalsIgnoreCase(parts[1].trim())) dir = "desc";
    }
    return tenantUserRoleRepository.searchTenantAdmins(nameLike, field, dir, size, offset);
  }

  @Transactional(readOnly = true)
  public Optional<TenantAdminGlobalRow> findTenantAdmin(UserId userId) {
    Objects.requireNonNull(userId, "userId");
    return tenantUserRoleRepository.findTenantAdminByUserId(userId.value());
  }

  @Transactional(readOnly = true)
  public long countTenantAdmins(String q) {
    String nameLike = (q == null || q.isBlank()) ? null : "%" + q.trim().toLowerCase() + "%";
    return tenantUserRoleRepository.countTenantAdmins(nameLike);
  }

  @Transactional
  public void assignSuperAdmin(UserId userId, UserId assignedBy) {
    var role = appRoleRepository.findActiveSystemRoleByCodeAndScope(SUPER_ADMIN_ROLE, PLATFORM_ROLE_SCOPE)
        .orElseThrow(() -> new IllegalStateException("Platform role not found: " + SUPER_ADMIN_ROLE));
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
  public void removeSuperAdmin(UserId userId) {
    var role = appRoleRepository.findActiveSystemRoleByCodeAndScope(SUPER_ADMIN_ROLE, PLATFORM_ROLE_SCOPE)
        .orElseThrow(() -> new IllegalStateException("Platform role not found: " + SUPER_ADMIN_ROLE));
    platformUserRoleRepository.softDeleteAssignment(userId.value(), role.getId());
  }
}
