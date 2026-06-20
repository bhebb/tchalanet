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
      from PlatformUserRoleJpaEntity assignment
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
      from PlatformUserRoleJpaEntity assignment
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

  /**
   * DB-backed global access snapshot for an app user. Platform and tenant roles are returned in a
   * single projection so request access resolution and bootstrap can reason from the same source.
   *
   * <p>Seller terminal columns are reserved for a future app-user-to-terminal binding. V0 terminal
   * actors are still resolved from their dedicated actor mapping.
   */
  @Query(
      value = """
          select
            assignment.user_id as "userId",
            cast(null as uuid) as "tenantId",
            cast(null as varchar) as "tenantCode",
            cast(null as varchar) as "tenantName",
            cast(null as varchar) as "tenantStatus",
            'PLATFORM' as "scope",
            ar.code as "roleCode",
            rp.permission_code as "permissionCode",
            cast(null as uuid) as "sellerTerminalId",
            cast(null as varchar) as "terminalCode",
            cast(null as varchar) as "sellerTerminalStatus"
          from platform_user_role assignment
          join app_role ar on ar.id = assignment.role_id
          left join role_permission rp on rp.role_id = ar.id
          where assignment.user_id = :userId
            and assignment.deleted_at is null
            and ar.system = true
            and ar.active = true
            and ar.deleted_at is null
            and ar.scope = 'PLATFORM'
          union all
          select
            assignment.user_id as "userId",
            assignment.tenant_id as "tenantId",
            t.code as "tenantCode",
            t.name as "tenantName",
            t.status as "tenantStatus",
            'TENANT' as "scope",
            ar.code as "roleCode",
            rp.permission_code as "permissionCode",
            cast(null as uuid) as "sellerTerminalId",
            cast(null as varchar) as "terminalCode",
            cast(null as varchar) as "sellerTerminalStatus"
          from tenant_user_role assignment
          join app_role ar on ar.id = assignment.role_id
          join tenant t on t.id = assignment.tenant_id
          join tenant_user membership
            on membership.tenant_id = assignment.tenant_id
           and membership.user_id = assignment.user_id
           and membership.deleted_at is null
           and coalesce(membership.status, 'ACTIVE') = 'ACTIVE'
          left join role_permission rp on rp.role_id = ar.id
          where assignment.user_id = :userId
            and assignment.deleted_at is null
            and ar.active = true
            and ar.deleted_at is null
            and ar.scope = 'TENANT'
            and t.deleted_at is null
          """,
      nativeQuery = true)
  List<UserAccessRow> findUserAccessRows(@Param("userId") UUID userId);

  @Query("select r from TenantUserRoleJpaEntity r where r.tenantId = :tenantId and r.userId = :userId and r.roleId = :roleId and r.deletedAt is null")
  Optional<TenantUserRoleJpaEntity> findActiveAssignment(
      @Param("tenantId") UUID tenantId, @Param("userId") UUID userId, @Param("roleId") UUID roleId);

  @Modifying
  @Query("update TenantUserRoleJpaEntity r set r.deletedAt = current_timestamp where r.tenantId = :tenantId and r.userId = :userId and r.roleId = :roleId and r.deletedAt is null")
  int softDeleteAssignment(
      @Param("tenantId") UUID tenantId, @Param("userId") UUID userId, @Param("roleId") UUID roleId);
}
