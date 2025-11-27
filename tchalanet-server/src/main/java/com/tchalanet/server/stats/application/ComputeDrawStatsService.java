package com.tchalanet.server.stats.application;

import com.tchalanet.server.stats.domain.model.DrawStats;
import com.tchalanet.server.stats.domain.ports.in.ComputeDrawStatsUseCase;
import com.tchalanet.server.stats.domain.ports.out.StatsRepositoryPort;
import com.tchalanet.server.stats.domain.ports.out.TicketReadModelPort;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComputeDrawStatsService implements ComputeDrawStatsUseCase {

  private final TicketReadModelPort ticketReadModel;
  private final StatsRepositoryPort statsRepository;

  public ComputeDrawStatsService(
      TicketReadModelPort ticketReadModel, StatsRepositoryPort statsRepository) {
    this.ticketReadModel = ticketReadModel;
    this.statsRepository = statsRepository;
  }

  @Override
  @Transactional
  public void computeAndSaveStatsForDraw(UUID drawId) {
    List<TicketReadModelPort.TicketInfo> tickets = ticketReadModel.findTicketsByDrawId(drawId);

    if (tickets.isEmpty()) {
      // Optionally, save zero-stats to indicate it has been processed
      return;
    }

    // The tenantId will be the same for all tickets in a given draw.
    UUID tenantId = tickets.get(0).tenantId();

    long totalTickets = tickets.size();
    long totalLines = tickets.stream().mapToLong(TicketReadModelPort.TicketInfo::lineCount).sum();
    BigDecimal totalStake =
        tickets.stream()
            .map(TicketReadModelPort.TicketInfo::totalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalPayout =
        tickets.stream()
            .map(TicketReadModelPort.TicketInfo::totalPayout)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    long winnersCount =
        tickets.stream()
            .filter(t -> t.totalPayout() != null && t.totalPayout().compareTo(BigDecimal.ZERO) > 0)
            .count();

    DrawStats stats =
        DrawStats.calculate(
            drawId, tenantId, totalTickets, totalLines, totalStake, totalPayout, winnersCount);

    statsRepository.upsertDrawStats(stats);
  }
}
