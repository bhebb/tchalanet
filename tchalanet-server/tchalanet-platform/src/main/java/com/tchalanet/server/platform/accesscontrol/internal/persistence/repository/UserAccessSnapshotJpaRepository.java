package com.tchalanet.server.platform.accesscontrol.internal.persistence.repository;

import com.tchalanet.server.platform.accesscontrol.internal.persistence.entity.PlatformUserRoleJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Bootstrap-only: full platform + tenant access snapshot in one UNION query.
 * Used exclusively by {@code AccessControlSnapshotResolver#resolveUserAccess}.
 * Do NOT call on every API request — use the scoped resolvers instead.
 */
public interface UserAccessSnapshotJpaRepository
    extends Repository<PlatformUserRoleJpaEntity, UUID> {

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
}
