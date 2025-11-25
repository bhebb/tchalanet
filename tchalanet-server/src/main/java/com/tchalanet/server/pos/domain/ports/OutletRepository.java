package com.tchalanet.server.pos.domain.ports;

import com.tchalanet.server.pos.domain.model.Outlet;
import java.util.Optional;
import java.util.UUID;

public interface OutletRepository {
  Optional<Outlet> findById(UUID id);

  Outlet save(Outlet outlet);
}
