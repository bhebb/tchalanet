package com.tchalanet.server.features.stats.aggregates.app;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.core.draw.internal.domain.event.DrawResultAppliedEvent;
import com.tchalanet.server.features.stats.aggregates.persistence.StatsDrawEntity;
import com.tchalanet.server.features.stats.aggregates.persistence.StatsDrawJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StatsDrawUpdaterService {

    private final StatsDrawJpaRepository statsDrawRepo;
    private final ResultSlotCatalog resultSlotCatalog;

    @Transactional
    public void ensureDrawRow(DrawResultAppliedEvent event) {
        var existing = statsDrawRepo.findByDrawId(event.drawId().value());
        if (existing != null && !existing.isEmpty()) {
            return;
        }

        String gameCode = resultSlotCatalog.findById(event.resultSlotId())
            .map(ResultSlotView::slotKey)
            .orElse("UNKNOWN");

        var e =
            StatsDrawEntity.builder()
                .id(UUID.randomUUID())
                .drawId(event.drawId().value())
                .tenantId(event.tenantId().value())
                .gameCode(gameCode)
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
