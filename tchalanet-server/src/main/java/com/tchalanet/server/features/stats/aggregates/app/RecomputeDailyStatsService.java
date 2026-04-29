package com.tchalanet.server.features.stats.aggregates.app;

import com.tchalanet.server.features.stats.aggregates.RecomputeDailyRequest;
import com.tchalanet.server.features.stats.aggregates.persistence.StatsDailyEntity;
import com.tchalanet.server.features.stats.aggregates.persistence.StatsDailyJpaRepository;
import com.tchalanet.server.features.stats.aggregates.persistence.SalesReportingReader;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RecomputeDailyStatsService {

  private final StatsDailyJpaRepository statsDailyRepo;
  private final SalesReportingReader salesReportingReader;

  @Transactional
  public Void handle(RecomputeDailyRequest command) {
    var from = command.from();
    var to = command.to();

    // 1) delete existing stats in the range
    statsDailyRepo.deleteByRefDateBetween(from, to);

    // 2) compute tenant-level aggregates and insert
    var tenantRows = salesReportingReader.listDailyTenantStats(from, to);
    for (var r : tenantRows) {
      StatsDailyEntity e =
          StatsDailyEntity.builder()
              .id(UUID.randomUUID())
              .dimensionType("tenant")
              .dimensionId(r.tenantId())
              .refDate(r.refDate())
              .ticketsCount(r.ticketsCount())
              .ticketsCancelledCount(r.ticketsCancelledCount())
              .stakeSumCents(r.stakeSumCents())
              .winningsSumCents(r.winningsSumCents())
              .netRevenueCents(r.netRevenueCents())
              .payoutsCount(r.payoutsCount())
              .sessionsOpenedCount(r.sessionsOpenedCount())
              .sessionsClosedCount(r.sessionsClosedCount())
              .version(0L)
              .createdAt(Instant.now())
              .updatedAt(Instant.now())
              .build();
      statsDailyRepo.save(e);
    }

    return null;
  }
}
