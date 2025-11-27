package com.tchalanet.server.accesscontrol.infra.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RolePermissionAdminJpaRepository
    extends JpaRepository<AppRolePermissionEntity, AppRolePermissionId> {

  @Query(
      """
           select arp from AppRolePermissionEntity arp
           join fetch arp.permission p
           where arp.role.id = :roleId
           """)
  List<AppRolePermissionEntity> findByRoleId(@Param("roleId") UUID roleId);

  void deleteByRoleId(UUID roleId);
}
