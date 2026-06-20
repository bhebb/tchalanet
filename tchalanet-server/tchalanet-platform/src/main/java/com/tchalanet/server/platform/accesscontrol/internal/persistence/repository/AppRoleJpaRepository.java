package com.tchalanet.server.platform.accesscontrol.internal.persistence.repository;

import com.tchalanet.server.platform.accesscontrol.internal.persistence.entity.AppRoleJpaEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppRoleJpaRepository extends JpaRepository<AppRoleJpaEntity, UUID> {

  @Query(
      """
            select r from AppRoleJpaEntity r
            where r.tenantId is null
              and r.deletedAt is null
            order by r.code
            """)
  List<AppRoleJpaEntity> findAllGlobalNotDeleted();

  @Query(
      """
            select r from AppRoleJpaEntity r
            where (r.tenantId is null or r.tenantId = :tenantId)
              and r.deletedAt is null
            order by r.tenantId nulls first, r.code
            """)
  List<AppRoleJpaEntity> findAllForTenantOrGlobal(UUID tenantId);

  Optional<AppRoleJpaEntity> findByCode(String code);

  @Query(
      """
            select r from AppRoleJpaEntity r
            where r.tenantId is null
              and r.code = :code
              and r.scope = :scope
              and r.system = true
              and r.active = true
              and r.deletedAt is null
            """)
  Optional<AppRoleJpaEntity> findActiveSystemRoleByCodeAndScope(
      @Param("code") String code,
      @Param("scope") String scope);

  @Query(
      """
            select r from AppRoleJpaEntity r
            where r.tenantId = :tenantId
              and r.code = :code
              and r.deletedAt is null
            """)
  Optional<AppRoleJpaEntity> findTenantRole(UUID tenantId, String code);
}
