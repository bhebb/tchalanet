package com.tchalanet.server.core.outlet.infra.persistence;

import com.tchalanet.server.core.outlet.application.port.out.OutletRepositoryPort;
import com.tchalanet.server.core.outlet.domain.model.Outlet;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutletPersistenceAdapter implements OutletRepositoryPort {

  private final OutletSpringRepository repo;

  @Override
  public Optional<Outlet> findById(UUID id, UUID tenantId) {
    return repo.findById(id).filter(e -> tenantId.equals(e.getTenantId())).map(OutletEntity::toDomain);
  }

  @Override
  public List<Outlet> findByTenantId(UUID tenantId) {
    return repo.findByTenantId(tenantId).stream().map(OutletEntity::toDomain).collect(Collectors.toList());
  }
}

