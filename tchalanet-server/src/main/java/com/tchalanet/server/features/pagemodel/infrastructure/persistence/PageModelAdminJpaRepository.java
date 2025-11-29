package com.tchalanet.server.features.pagemodel.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PageModelAdminJpaRepository extends JpaRepository<PageModelEntity, UUID> {

  @Query(
      """
           select pm from PageModelEntity pm
           where (:tenantId is null or pm.tenantId = :tenantId)
             and (:code is null or pm.code = :code)
             and (:lang is null or pm.lang = :lang)
             and pm.deletedAt is null
           """)
  Page<PageModelEntity> search(
      @Param("tenantId") UUID tenantId,
      @Param("code") String code,
      @Param("lang") String lang,
      Pageable pageable);

  boolean existsByTenantIdAndCodeAndLang(UUID tenantId, String code, String lang);
}
