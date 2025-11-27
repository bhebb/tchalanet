package com.tchalanet.server.draw.domain.ports.out;

import com.tchalanet.server.draw.domain.model.Draw;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DrawRepositoryPort {

  Optional<Draw> findById(UUID id);

  List<Draw> findByTenantAndChannelAndScheduledAtBetween(
      UUID tenantId, UUID drawChannelId, Instant from, Instant to);

  Draw save(Draw draw);
}
