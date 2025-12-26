package com.tchalanet.server.core.outlet.infra.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutletSpringRepository extends JpaRepository<OutletEntity, UUID> {
  List<OutletEntity> findByTenantId(UUID tenantId);

  Optional<OutletEntity> findByTenantIdAndSlug(UUID tenantId, String slug);

  List<OutletEntity> findByTenantIdAndSalesBlocked(UUID tenantId, boolean salesBlocked);
}
