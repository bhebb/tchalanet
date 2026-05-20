package com.tchalanet.server.platform.accesscontrol.internal.persistence.repository;

import com.tchalanet.server.platform.accesscontrol.internal.persistence.entity.AppRolePermissionJpaEntity;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.entity.AppRolePermissionId;

import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RolePermissionJpaRepository
    extends JpaRepository<AppRolePermissionJpaEntity, AppRolePermissionId> {

  @Query(
      "select arp from AppRolePermissionJpaEntity arp "
          + "join fetch arp.role r "
          + "join fetch arp.permission p "
          + "where r.code in :roleCodes")
  List<AppRolePermissionJpaEntity> findByRoleCodes(@Param("roleCodes") Set<String> roleCodes);
}

