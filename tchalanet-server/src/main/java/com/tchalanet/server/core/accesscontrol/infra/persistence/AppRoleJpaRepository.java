package com.tchalanet.server.core.accesscontrol.infra.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AppRoleJpaRepository extends JpaRepository<AppRoleEntity, UUID> {

  @Query(
      """
            select r from AppRoleEntity r
            where r.tenantId is null
              and r.deletedAt is null
            order by r.code
            """)
  List<AppRoleEntity> findAllGlobalNotDeleted();

  @Query(
      """
            select r from AppRoleEntity r
            where (r.tenantId is null or r.tenantId = :tenantId)
              and r.deletedAt is null
            order by r.tenantId nulls first, r.code
            """)
  List<AppRoleEntity> findAllForTenantOrGlobal(UUID tenantId);

  Optional<AppRoleEntity> findByCode(String code);

  @Query(
      """
            select r from AppRoleEntity r
            where r.tenantId = :tenantId
              and r.code = :code
              and r.deletedAt is null
            """)
  Optional<AppRoleEntity> findTenantRole(UUID tenantId, String code);
}
