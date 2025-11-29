package com.tchalanet.server.core.limitpolicy.domain.ports.out;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound Port for reading game information from the Game domain. This avoids direct dependency on
 * Game domain's persistence.
 */
public interface GameReadModelPort {
  Optional<GameInfo> findGameInfoByCode(String gameCode);

  record GameInfo(UUID id, String code, String name) {}
}
