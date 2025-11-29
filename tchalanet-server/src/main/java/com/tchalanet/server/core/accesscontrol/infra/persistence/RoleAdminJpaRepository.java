package com.tchalanet.server.core.accesscontrol.infra.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RoleAdminJpaRepository extends JpaRepository<AppRoleEntity, UUID> {

  boolean existsByCode(String code);

  @Query("select r from AppRoleEntity r where r.deletedAt is null")
  List<AppRoleEntity> findAllActive();
}
