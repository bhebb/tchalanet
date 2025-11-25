package com.tchalanet.server.pos.domain.ports;

import com.tchalanet.server.pos.domain.model.Terminal;
import java.util.Optional;
import java.util.UUID;

public interface TerminalRepository {
  Optional<Terminal> findById(UUID id);

  Terminal save(Terminal t);
}
