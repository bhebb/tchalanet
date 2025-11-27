package com.tchalanet.server.ticket.domain.ports.out;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface DrawResolutionPort {
  record Draw(UUID id, Instant scheduledAt, int cutoffSec) {}

  Optional<Draw> findById(UUID id);
}
