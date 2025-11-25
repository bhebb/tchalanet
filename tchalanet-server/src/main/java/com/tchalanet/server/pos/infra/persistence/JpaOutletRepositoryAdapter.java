package com.tchalanet.server.pos.infra.persistence;

import com.tchalanet.server.pos.domain.model.Outlet;
import com.tchalanet.server.pos.domain.ports.OutletRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaOutletRepositoryAdapter implements OutletRepository {

  private final OutletJpaRepository jpa;

  @Override
  public Optional<Outlet> findById(UUID id) {
    return jpa.findById(id)
        .map(e -> new Outlet(e.getId(), e.getTenantId(), e.getName(), e.getZone()));
  }

  @Override
  public Outlet save(Outlet outlet) {
    var e = new OutletJpaEntity();
    e.setId(outlet.id());
    e.setTenantId(outlet.tenantId());
    e.setName(outlet.name());
    e.setZone(outlet.zone());
    var saved = jpa.save(e);
    return new Outlet(saved.getId(), saved.getTenantId(), saved.getName(), saved.getZone());
  }
}
