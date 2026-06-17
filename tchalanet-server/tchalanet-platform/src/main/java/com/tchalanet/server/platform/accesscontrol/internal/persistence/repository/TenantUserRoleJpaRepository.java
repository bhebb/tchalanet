package com.tchalanet.server.platform.accesscontrol.internal.persistence.repository;

import com.tchalanet.server.platform.accesscontrol.internal.persistence.entity.TenantUserRoleJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TenantUserRoleJpaRepository extends JpaRepository<TenantUserRoleJpaEntity, UUID> {

  @Query("select r from TenantUserRoleJpaEntity r where r.tenantId = :tenantId and r.userId = :userId and r.deletedAt is null")
  List<TenantUserRoleJpaEntity> findActiveByTenantAndUser(
      @Param("tenantId") UUID tenantId, @Param("userId") UUID userId);

  @Query("""
      select distinct role.id
      from TenantUserRoleJpaEntity assignment
      join AppRoleJpaEntity role on role.id = assignment.roleId
      where assignment.userId = :userId
        and assignment.deletedAt is null
        and role.system = true
        and role.active = true
        and role.deletedAt is null
        and role.scope = 'PLATFORM'
      """)
  List<UUID> findActivePlatformRoleIdsByUser(@Param("userId") UUID userId);

  @Query("""
      select distinct assignment.tenantId
      from TenantUserRoleJpaEntity assignment
      join AppRoleJpaEntity role on role.id = assignment.roleId
      where assignment.userId = :userId
        and assignment.deletedAt is null
        and role.active = true
        and role.deletedAt is null
        and role.scope = 'TENANT'
      """)
  List<UUID> findDistinctActiveTenantIdsByUser(@Param("userId") UUID userId);

  /**
   * One-shot platform-scope access snapshot for a user: each active platform role joined to its
   * granted permissions (left join, so a role with no permissions still yields a row with a null
   * permission code). Replaces the role-id → role-entity lookup + per-role permission loop.
   */
  @Query("""
      select role.code as roleCode, rp.id.permissionCode as permissionCode
      from TenantUserRoleJpaEntity assignment
      join AppRoleJpaEntity role on role.id = assignment.roleId
      left join AppRolePermissionJpaEntity rp on rp.role.id = role.id
      where assignment.userId = :userId
        and assignment.deletedAt is null
        and role.system = true
        and role.active = true
        and role.deletedAt is null
        and role.scope = 'PLATFORM'
      """)
  List<RoleAccessRow> findPlatformRoleAccessRows(@Param("userId") UUID userId);

  /**
   * One-shot tenant-scope access snapshot for a user in a tenant: each active assigned role joined
   * to its granted permissions (left join). Role-level grants only; user GRANT/DENY overrides are
   * applied separately.
   */
  @Query("""
      select role.code as roleCode, rp.id.permissionCode as permissionCode
      from TenantUserRoleJpaEntity assignment
      join AppRoleJpaEntity role on role.id = assignment.roleId
      left join AppRolePermissionJpaEntity rp on rp.role.id = role.id
      where assignment.userId = :userId
        and assignment.tenantId = :tenantId
        and assignment.deletedAt is null
        and role.active = true
        and role.deletedAt is null
      """)
  List<RoleAccessRow> findTenantRoleAccessRows(
      @Param("tenantId") UUID tenantId, @Param("userId") UUID userId);

  @Query("select r from TenantUserRoleJpaEntity r where r.tenantId = :tenantId and r.userId = :userId and r.roleId = :roleId and r.deletedAt is null")
  Optional<TenantUserRoleJpaEntity> findActiveAssignment(
      @Param("tenantId") UUID tenantId, @Param("userId") UUID userId, @Param("roleId") UUID roleId);

  @Modifying
  @Query("update TenantUserRoleJpaEntity r set r.deletedAt = current_timestamp where r.tenantId = :tenantId and r.userId = :userId and r.roleId = :roleId and r.deletedAt is null")
  int softDeleteAssignment(
      @Param("tenantId") UUID tenantId, @Param("userId") UUID userId, @Param("roleId") UUID roleId);
}
