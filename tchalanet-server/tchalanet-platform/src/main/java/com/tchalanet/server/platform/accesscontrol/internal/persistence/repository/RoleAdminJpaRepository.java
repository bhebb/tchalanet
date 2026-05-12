package com.tchalanet.server.platform.accesscontrol.internal.persistence.repository;

import com.tchalanet.server.platform.accesscontrol.internal.persistence.entity.AppRoleJpaEntity;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RoleAdminJpaRepository extends JpaRepository<AppRoleJpaEntity, UUID> {

  boolean existsByCode(String code);

  @Query("select r from AppRoleJpaEntity r where r.deletedAt is null")
  List<AppRoleJpaEntity> findAllActive();
}

