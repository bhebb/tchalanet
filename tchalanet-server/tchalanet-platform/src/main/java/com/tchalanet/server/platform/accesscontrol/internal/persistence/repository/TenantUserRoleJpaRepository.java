package com.tchalanet.server.platform.accesscontrol.internal.persistence.repository;

import com.tchalanet.server.platform.accesscontrol.internal.persistence.entity.TenantUserRoleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantUserRoleJpaRepository extends JpaRepository<TenantUserRoleJpaEntity, UUID> {

    @Query("select r from TenantUserRoleJpaEntity r where r.tenantId = :tenantId and r.userId = :userId and r.deletedAt is null")
    List<TenantUserRoleJpaEntity> findActiveByTenantAndUser(
        @Param("tenantId") UUID tenantId, @Param("userId") UUID userId);

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
          and role.scope = 'TENANT'
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

    /**
     * Global TENANT_ADMIN search across all tenants (SUPER_ADMIN only).
     * Filters by display_name/email ILIKE when :nameLike is not null.
     */
    @Query(value = """
        select
          u.id                                                              as "userId",
          u.email::text                                                     as "email",
          coalesce(nullif(u.display_name,''), u.username, u.email::text)   as "displayName",
          u.status                                                          as "status",
          tur.tenant_id                                                     as "tenantId",
          t.name                                                            as "tenantName",
          t.code                                                            as "tenantCode",
          tur.assigned_at                                                   as "assignedAt"
        from tenant_user_role tur
        join app_role ar on ar.id = tur.role_id
          and ar.code = 'TENANT_ADMIN'
          and ar.scope = 'TENANT'
          and ar.active = true
          and ar.deleted_at is null
        join app_user u on u.id = tur.user_id and u.deleted_at is null
        join tenant t on t.id = tur.tenant_id and t.deleted_at is null
        where tur.deleted_at is null
          and (cast(:nameLike as text) is null
               or lower(coalesce(u.display_name, '')) like :nameLike
               or lower(u.email::text)                like :nameLike)
        order by
          case when cast(:sortDir as text) = 'asc' then
            case cast(:sortField as text)
              when 'status'    then u.status
              when 'createdAt' then to_char(tur.assigned_at, 'YYYY-MM-DD"T"HH24:MI:SS"Z"')
              else lower(coalesce(nullif(u.display_name, ''), u.email::text))
            end
          end asc  nulls last,
          case when cast(:sortDir as text) = 'desc' then
            case cast(:sortField as text)
              when 'status'    then u.status
              when 'createdAt' then to_char(tur.assigned_at, 'YYYY-MM-DD"T"HH24:MI:SS"Z"')
              else lower(coalesce(nullif(u.display_name, ''), u.email::text))
            end
          end desc nulls last
        limit :limit offset :offset
        """, nativeQuery = true)
    List<TenantAdminGlobalRow> searchTenantAdmins(
        @Param("nameLike")  String nameLike,
        @Param("sortField") String sortField,
        @Param("sortDir")   String sortDir,
        @Param("limit")     int limit,
        @Param("offset")    int offset);

    @Query(value = """
        select
          u.id                                                              as "userId",
          u.email::text                                                     as "email",
          coalesce(nullif(u.display_name,''), u.username, u.email::text)   as "displayName",
          u.status                                                          as "status",
          tur.tenant_id                                                     as "tenantId",
          t.name                                                            as "tenantName",
          t.code                                                            as "tenantCode",
          tur.assigned_at                                                   as "assignedAt"
        from tenant_user_role tur
        join app_role ar on ar.id = tur.role_id
          and ar.code = 'TENANT_ADMIN'
          and ar.scope = 'TENANT'
          and ar.active = true
          and ar.deleted_at is null
        join app_user u on u.id = tur.user_id and u.deleted_at is null
        join tenant t on t.id = tur.tenant_id and t.deleted_at is null
        where tur.deleted_at is null
          and u.id = :userId
        limit 1
        """, nativeQuery = true)
    Optional<TenantAdminGlobalRow> findTenantAdminByUserId(@Param("userId") UUID userId);

    @Query(value = """
        select count(*)
        from tenant_user_role tur
        join app_role ar on ar.id = tur.role_id
          and ar.code = 'TENANT_ADMIN'
          and ar.scope = 'TENANT'
          and ar.active = true
          and ar.deleted_at is null
        join app_user u on u.id = tur.user_id and u.deleted_at is null
        join tenant t on t.id = tur.tenant_id and t.deleted_at is null
        where tur.deleted_at is null
          and (cast(:nameLike as text) is null
               or lower(coalesce(u.display_name, '')) like :nameLike
               or lower(u.email::text)                like :nameLike)
        """, nativeQuery = true)
    long countTenantAdmins(@Param("nameLike") String nameLike);
}
