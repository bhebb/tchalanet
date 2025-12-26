package com.tchalanet.server.features.stats.aggregates.application;

import com.tchalanet.server.core.draw.domain.event.DrawResultedEvent;
import com.tchalanet.server.features.stats.aggregates.persistence.StatsDrawEntity;
import com.tchalanet.server.features.stats.aggregates.persistence.StatsDrawJpaRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class StatsDrawUpdater {

  private final StatsDrawJpaRepository statsDrawRepo;

  @Transactional
  public void ensureDrawRow(DrawResultedEvent event) {
    var existing = statsDrawRepo.findByDrawId(event.drawId().uuid());
    if (existing == null || existing.isEmpty()) {
      var e =
          StatsDrawEntity.builder()
              .id(UUID.randomUUID())
              .drawId(event.drawId().uuid())
              .tenantId(event.tenantId().value())
              .gameCode(event.gameCode())
              .scheduledAt(event.scheduledAt())
              .ticketsCount(0L)
              .stakeSumCents(0L)
              .winningsSumCents(0L)
              .netRevenueCents(0L)
              .createdAt(Instant.now())
              .updatedAt(Instant.now())
              .build();
      statsDrawRepo.save(e);
    }
  }
}
