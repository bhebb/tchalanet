package com.tchalanet.server.core.outlet.infra.rest.outlet;

import com.tchalanet.server.core.outlet.infra.persistence.OutletEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "outlets", collectionResourceRel = "outlets")
public interface OutletRestRepository extends JpaRepository<OutletEntity, UUID> {}
