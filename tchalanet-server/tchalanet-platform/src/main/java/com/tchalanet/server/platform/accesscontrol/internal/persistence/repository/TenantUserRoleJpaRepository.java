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

  @Query("select r from TenantUserRoleJpaEntity r where r.tenantId = :tenantId and r.userId = :userId and r.roleId = :roleId and r.deletedAt is null")
  Optional<TenantUserRoleJpaEntity> findActiveAssignment(
      @Param("tenantId") UUID tenantId, @Param("userId") UUID userId, @Param("roleId") UUID roleId);

  @Modifying
  @Query("update TenantUserRoleJpaEntity r set r.deletedAt = current_timestamp where r.tenantId = :tenantId and r.userId = :userId and r.roleId = :roleId and r.deletedAt is null")
  int softDeleteAssignment(
      @Param("tenantId") UUID tenantId, @Param("userId") UUID userId, @Param("roleId") UUID roleId);
}
