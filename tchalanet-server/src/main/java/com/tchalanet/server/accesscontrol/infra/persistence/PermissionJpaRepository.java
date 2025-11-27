package com.tchalanet.server.accesscontrol.infra.persistence;

import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PermissionJpaRepository extends JpaRepository<PermissionEntity, String> {

  @Query("select p from PermissionEntity p where p.deletedAt is null")
  List<PermissionEntity> findAllNotDeleted();

  List<PermissionEntity> findAllByCodeIn(Set<String> codes);

  @Query("select p from PermissionEntity p where p.deletedAt is null")
  Page<PermissionEntity> findAllNotDeleted(Pageable pageable);

  @Query(
      "select p from PermissionEntity p where p.deletedAt is null and (lower(p.code) like lower(concat('%', :q, '%')) or lower(p.name) like lower(concat('%', :q, '%')) or lower(p.description) like lower(concat('%', :q, '%')))")
  Page<PermissionEntity> searchActive(@Param("q") String query, Pageable pageable);
}
