package com.tchalanet.server.core.outlet.infra.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "outlets", collectionResourceRel = "outlets")
public interface OutletSpringRepository extends JpaRepository<OutletEntity, UUID> {
    List<OutletEntity> findByTenantId(UUID tenantId);
}

