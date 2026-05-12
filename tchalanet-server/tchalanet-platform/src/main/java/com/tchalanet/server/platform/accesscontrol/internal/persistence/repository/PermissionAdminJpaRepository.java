package com.tchalanet.server.platform.accesscontrol.internal.persistence.repository;

import com.tchalanet.server.platform.accesscontrol.internal.persistence.entity.PermissionJpaEntity;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;

public interface PermissionAdminJpaRepository extends JpaRepository<PermissionJpaEntity, String> {

  Optional<PermissionJpaEntity> findByCode(String code);

  @Query("select p from PermissionJpaEntity p where p.deletedAt is null")
  @RestResource(path = "all-active", rel = "all-active")
  List<PermissionJpaEntity> findAllActive();

  @Query("select p from PermissionJpaEntity p where p.deletedAt is null")
  @RestResource(path = "active", rel = "active")
  Page<PermissionJpaEntity> findAllActive(Pageable pageable);

  @Query(
      "select p from PermissionJpaEntity p where p.deletedAt is null and "
          + "(lower(p.code) like lower(concat('%', :q, '%')) "
          + " or lower(p.name) like lower(concat('%', :q, '%')) "
          + " or lower(p.description) like lower(concat('%', :q, '%'))) ")
  Page<PermissionJpaEntity> searchActive(@Param("q") String query, Pageable pageable);
}

