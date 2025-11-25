package com.tchalanet.server.draw.domain.ports;

import com.tchalanet.server.draw.domain.model.Odds;
import java.util.Optional;
import java.util.UUID;

public interface OddsRepository {
  Optional<Odds> findById(UUID id);

  Odds save(Odds odds);
}
