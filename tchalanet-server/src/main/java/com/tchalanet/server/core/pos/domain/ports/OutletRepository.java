package com.tchalanet.server.core.pos.domain.ports;

import com.tchalanet.server.core.pos.domain.model.Outlet;
import java.util.Optional;
import java.util.UUID;

public interface OutletRepository {
  Optional<Outlet> findById(UUID id);

  Outlet save(Outlet outlet);
}
