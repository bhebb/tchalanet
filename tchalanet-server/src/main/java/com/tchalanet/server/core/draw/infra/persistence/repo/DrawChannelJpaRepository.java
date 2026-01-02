package com.tchalanet.server.core.draw.infra.persistence.repo;

import com.tchalanet.server.core.draw.infra.persistence.DrawChannelJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DrawChannelJpaRepository extends JpaRepository<DrawChannelJpaEntity, UUID> {

  Optional<DrawChannelJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

  Optional<DrawChannelJpaEntity> findByTenantIdAndCode(UUID tenantId, String code);

  List<DrawChannelJpaEntity> findByTenantIdOrderBySortOrderAsc(UUID tenantId);

  List<DrawChannelJpaEntity> findByActiveTrueOrderBySortOrder();

  List<DrawChannelJpaEntity> findByTenantIdAndActiveTrueOrderBySortOrderAsc(UUID tenantId);

  List<DrawChannelJpaEntity> findByTenantIdAndActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc(
      UUID tenantId);

  @EntityGraph(attributePaths = {"draws"})
  @org.springframework.data.jpa.repository.Query(
      "select c from DrawChannelJpaEntity c where c.tenantId = :tenantId and c.code = :code")
  Optional<DrawChannelJpaEntity> findByTenantIdAndCodeFetchDraws(
      @org.springframework.data.repository.query.Param("tenantId") UUID tenantId,
      @org.springframework.data.repository.query.Param("code") String code);
}
