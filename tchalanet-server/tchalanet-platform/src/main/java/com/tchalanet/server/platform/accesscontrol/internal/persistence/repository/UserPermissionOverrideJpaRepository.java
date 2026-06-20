package com.tchalanet.server.platform.accesscontrol.internal.persistence.repository;

import com.tchalanet.server.platform.accesscontrol.internal.persistence.entity.UserPermissionOverrideJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserPermissionOverrideJpaRepository
    extends JpaRepository<UserPermissionOverrideJpaEntity, UUID> {

  @Query("select o from UserPermissionOverrideJpaEntity o where o.tenantId = :tenantId and o.userId = :userId and o.deletedAt is null")
  List<UserPermissionOverrideJpaEntity> findActiveByTenantAndUser(
      @Param("tenantId") UUID tenantId, @Param("userId") UUID userId);

  @Query("select o from UserPermissionOverrideJpaEntity o where o.userId = :userId and o.deletedAt is null")
  List<UserPermissionOverrideJpaEntity> findActiveByUser(@Param("userId") UUID userId);

  @Modifying
  @Query("update UserPermissionOverrideJpaEntity o set o.deletedAt = current_timestamp where o.tenantId = :tenantId and o.userId = :userId and o.permissionCode = :code and o.deletedAt is null")
  int softDelete(
      @Param("tenantId") UUID tenantId,
      @Param("userId") UUID userId,
      @Param("code") String permissionCode);
}
