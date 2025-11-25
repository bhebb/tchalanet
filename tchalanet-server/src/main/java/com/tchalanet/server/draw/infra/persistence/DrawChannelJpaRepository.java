package com.tchalanet.server.draw.infra.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DrawChannelJpaRepository extends JpaRepository<DrawChannelJpaEntity, UUID> {
  List<DrawChannelJpaEntity> findByTenantIdAndActiveTrueOrderBySortOrder(UUID tenantId);

  List<DrawChannelJpaEntity> findByActiveTrueOrderBySortOrder();
}
