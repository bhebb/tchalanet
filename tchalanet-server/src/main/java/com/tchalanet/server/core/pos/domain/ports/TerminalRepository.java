package com.tchalanet.server.core.pos.domain.ports;

import com.tchalanet.server.core.pos.domain.model.Terminal;
import java.util.Optional;
import java.util.UUID;

public interface TerminalRepository {
  Optional<Terminal> findById(UUID id);

  Terminal save(Terminal t);
}
