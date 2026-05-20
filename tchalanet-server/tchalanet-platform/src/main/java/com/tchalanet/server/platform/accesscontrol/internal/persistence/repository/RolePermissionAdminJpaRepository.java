package com.tchalanet.server.platform.accesscontrol.internal.persistence.repository;

import com.tchalanet.server.platform.accesscontrol.internal.persistence.entity.AppRolePermissionId;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.entity.AppRolePermissionJpaEntity;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RolePermissionAdminJpaRepository
    extends JpaRepository<AppRolePermissionJpaEntity, AppRolePermissionId> {

  @Query(
      """
           select arp from AppRolePermissionJpaEntity arp
           join fetch arp.permission p
           where arp.role.id = :roleId
           """)
  List<AppRolePermissionJpaEntity> findByRoleId(@Param("roleId") UUID roleId);

  void deleteByRoleId(UUID roleId);
}

