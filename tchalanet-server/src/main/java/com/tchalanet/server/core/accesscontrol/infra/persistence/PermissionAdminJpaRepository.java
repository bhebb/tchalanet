package com.tchalanet.server.core.accesscontrol.infra.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PermissionAdminJpaRepository extends JpaRepository<PermissionEntity, String> {

  Optional<PermissionEntity> findByCode(String code);

  @Query("select p from PermissionEntity p where p.deletedAt is null")
  List<PermissionEntity> findAllActive();

  @Query("select p from PermissionEntity p where p.deletedAt is null")
  Page<PermissionEntity> findAllActive(Pageable pageable);

  @Query(
      "select p from PermissionEntity p where p.deletedAt is null and "
          + "(lower(p.code) like lower(concat('%', :q, '%')) "
          + " or lower(p.name) like lower(concat('%', :q, '%')) "
          + " or lower(p.description) like lower(concat('%', :q, '%'))) ")
  Page<PermissionEntity> searchActive(@Param("q") String query, Pageable pageable);
}
