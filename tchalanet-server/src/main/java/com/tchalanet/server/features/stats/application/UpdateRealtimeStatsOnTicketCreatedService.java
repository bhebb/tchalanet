package com.tchalanet.server.features.stats.application;

import com.tchalanet.server.features.stats.domain.ports.in.UpdateRealtimeStatsOnTicketCreatedUseCase;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateRealtimeStatsOnTicketCreatedService
    implements UpdateRealtimeStatsOnTicketCreatedUseCase {

  @Override
  public void updateStats(UUID ticketId, UUID tenantId, UUID sessionId) {
    // As per requirement: Option 1 (simple): do nothing now, and only rely on draw_stats + nightly
    // aggregation.
    // This method serves as an extension point for future real-time counters (e.g., in-memory,
    // Redis).
    log.debug(
        "Real-time stats update for ticket {} (session {}) is currently a no-op.",
        ticketId,
        sessionId);
  }
}
