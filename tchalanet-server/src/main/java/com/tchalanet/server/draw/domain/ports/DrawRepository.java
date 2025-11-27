package com.tchalanet.server.draw.domain.ports;

import com.tchalanet.server.draw.domain.model.Draw;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DrawRepository {
  Optional<Draw> findById(UUID id);

  Draw save(Draw draw);

  List<Draw> findByTenantAndScheduledAtBetween(UUID tenantId, Instant from, Instant to);

  boolean existsByTenantChannelAndScheduledAt(
      UUID tenantId, UUID drawChannelId, Instant scheduledAt);

  /**
   * Atomically insert the draw if it does not already exist (tenant+channel+scheduled_at). Returns
   * true if inserted, false if already existed.
   */
  boolean saveIfNotExists(Draw draw);

  // Helper: find draws with given status scheduled before 'before' (excluding deleted)
  List<Draw> findByStatusAndScheduledAtBefore(String status, Instant before);

  /**
   * Finds all SCHEDULED draws for a tenant that are past their cutoff time.
   *
   * @param tenantId The tenant ID.
   * @param now The current instant.
   * @return A list of draws that should be closed.
   */
  List<Draw> findScheduledDrawsPastCutoff(UUID tenantId, Instant now);
}
