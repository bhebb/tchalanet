package com.tchalanet.server.features.stats.aggregates.app;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.core.draw.api.event.DrawResultAppliedEvent;
import com.tchalanet.server.features.stats.aggregates.persistence.StatsDrawEntity;
import com.tchalanet.server.features.stats.aggregates.persistence.StatsDrawJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class StatsDrawUpdaterService {

    private final StatsDrawJpaRepository statsDrawRepo;
    private final ResultSlotCatalog resultSlotCatalog;

    @Transactional
    public void ensureDrawRow(DrawResultAppliedEvent event) {
        if (statsDrawRepo.existsByDrawId(event.drawId().value())) {
            return;
        }

        String gameCode = resultSlotCatalog.findById(event.resultSlotId())
            .map(ResultSlotView::slotKey)
            .orElse("UNKNOWN");

        var e =
            StatsDrawEntity.builder()
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

        try {
            statsDrawRepo.save(e);
        } catch (DataIntegrityViolationException ex) {
            log.debug("stats.draw already exists drawId={}, skipping insert", event.drawId(), ex);
        }
    }
}
