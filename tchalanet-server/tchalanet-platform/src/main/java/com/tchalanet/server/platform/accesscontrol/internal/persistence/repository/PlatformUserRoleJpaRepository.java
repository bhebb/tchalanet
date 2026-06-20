package com.tchalanet.server.platform.accesscontrol.internal.persistence.repository;

import com.tchalanet.server.platform.accesscontrol.internal.persistence.entity.PlatformUserRoleJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlatformUserRoleJpaRepository extends JpaRepository<PlatformUserRoleJpaEntity, UUID> {

  @Query("""
      select r
      from PlatformUserRoleJpaEntity r
      where r.userId = :userId
        and r.roleId = :roleId
        and r.deletedAt is null
      """)
  Optional<PlatformUserRoleJpaEntity> findActiveAssignment(
      @Param("userId") UUID userId,
      @Param("roleId") UUID roleId);

  @Modifying
  @Query("""
      update PlatformUserRoleJpaEntity r
      set r.deletedAt = current_timestamp
      where r.userId = :userId
        and r.roleId = :roleId
        and r.deletedAt is null
      """)
  int softDeleteAssignment(@Param("userId") UUID userId, @Param("roleId") UUID roleId);

  @Query(
      value = """
          select
            u.id as "userId",
            u.email::text as "email",
            coalesce(nullif(u.display_name, ''), u.username, u.email::text) as "displayName",
            u.status as "status",
            pur.assigned_at as "assignedAt"
          from platform_user_role pur
          join app_role ar on ar.id = pur.role_id
          join app_user u on u.id = pur.user_id
          where pur.deleted_at is null
            and ar.code = 'SUPER_ADMIN'
            and ar.scope = 'PLATFORM'
            and ar.tenant_id is null
            and ar.active = true
            and ar.deleted_at is null
            and u.deleted_at is null
          order by lower(coalesce(u.display_name, u.email::text, u.username))
          """,
      nativeQuery = true)
  List<PlatformSuperAdminRow> listActiveSuperAdmins();
}
