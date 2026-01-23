package com.tchalanet.server.features.stats.aggregates.application;

import com.tchalanet.server.core.drawresult.domain.event.DrawResultedEvent;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.core.draw.application.port.out.DrawLookupPort;
import com.tchalanet.server.features.stats.aggregates.persistence.StatsDrawEntity;
import com.tchalanet.server.features.stats.aggregates.persistence.StatsDrawJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StatsDrawUpdater {

    private final StatsDrawJpaRepository statsDrawRepo;
    private final DrawLookupPort drawLookupPort;

    @Transactional
    public void ensureDrawRow(DrawResultedEvent event) {
        var drawId =
            drawLookupPort.findDrawIdBySlotId(event.tenantId(), event.drawDate(), ResultSlotId.of(event.drawResultId()));
        if (drawId.isEmpty()) {
            return;
        }

        var existing = statsDrawRepo.findByDrawId(drawId.get().value());
        if (existing == null || existing.isEmpty()) {
            var e =
                StatsDrawEntity.builder()
                    .id(UUID.randomUUID())
                    .drawId(drawId.get().value())
                    .tenantId(event.tenantId().value())
                    .gameCode(event.slotKey())
                    .scheduledAt(event.occurredAt())
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
