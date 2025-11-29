package com.tchalanet.server.features.stats.domain.ports.in;

import java.util.UUID;

/**
 * Inbound Port for updating real-time statistics when a ticket is created. As per requirements,
 * this is Option 1 (do nothing now, rely on later aggregation) but provides an extension point.
 */
public interface UpdateRealtimeStatsOnTicketCreatedUseCase {
  void updateStats(UUID ticketId, UUID tenantId, UUID sessionId);
}
