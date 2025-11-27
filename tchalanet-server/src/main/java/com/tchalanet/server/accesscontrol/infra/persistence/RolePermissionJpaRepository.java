package com.tchalanet.server.accesscontrol.infra.persistence;

import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RolePermissionJpaRepository
    extends JpaRepository<AppRolePermissionEntity, AppRolePermissionId> {

  @Query(
      "select arp from AppRolePermissionEntity arp "
          + "join fetch arp.role r "
          + "join fetch arp.permission p "
          + "where r.code in :roleCodes")
  List<AppRolePermissionEntity> findByRoleCodes(@Param("roleCodes") Set<String> roleCodes);
}
