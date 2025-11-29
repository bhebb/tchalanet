package com.tchalanet.server.features.stats.application;

import com.tchalanet.server.features.stats.domain.ports.in.ComputeSessionStatsUseCase;
import com.tchalanet.server.features.stats.domain.ports.out.StatsRepositoryPort;
import com.tchalanet.server.features.stats.domain.ports.out.TicketReadModelPort;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComputeSessionStatsService implements ComputeSessionStatsUseCase {

  private final TicketReadModelPort ticketReadModel;
  private final StatsRepositoryPort statsRepository;

  // private final PosSessionReadModelPort posSessionReadModel; // To get session details like
  // tenantId

  @Override
  @Transactional
  public void computeAndSaveStatsForSession(UUID sessionId) {
    log.info("Computing stats for session: {}", sessionId);

    // In a real scenario, you'd fetch tickets associated with this session
    // and aggregate their data. This would require a method like
    // ticketReadModel.findTicketsBySessionId(sessionId)
    // For now, this is a placeholder.

    // Example aggregation (placeholder values)
    UUID tenantId = UUID.randomUUID(); // Placeholder
    long totalTickets = 10;
    BigDecimal totalStake = new BigDecimal("100.00");
    BigDecimal totalPayout = new BigDecimal("50.00");
    BigDecimal grossMargin = totalStake.subtract(totalPayout);

    // You would then create a SessionStats domain object and upsert it
    // into the statsRepository. This requires a SessionStats model and corresponding entity/mapper.
    log.warn(
        "Session stats computation is a placeholder. Actual aggregation logic needs to be implemented.");

    // Example: statsRepository.upsertSessionStats(new SessionStats(...));
  }
}
