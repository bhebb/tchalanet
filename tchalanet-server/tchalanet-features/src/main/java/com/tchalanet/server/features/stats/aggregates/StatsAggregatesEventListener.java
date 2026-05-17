package com.tchalanet.server.features.stats.aggregates;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.core.draw.api.event.DrawResultAppliedEvent;
import com.tchalanet.server.core.sales.api.event.TicketCancelledEvent;
import com.tchalanet.server.core.sales.api.event.TicketPlacedEvent;
import com.tchalanet.server.core.sales.api.event.TicketResultedEvent;
import com.tchalanet.server.core.session.internal.domain.event.SalesSessionClosedEvent;
import com.tchalanet.server.core.session.internal.domain.event.SalesSessionOpenedEvent;
import com.tchalanet.server.features.stats.aggregates.app.StatsDailyUpdaterService;
import com.tchalanet.server.features.stats.aggregates.app.StatsDrawUpdaterService;
import com.tchalanet.server.features.stats.aggregates.persistence.StatsEventLogEntity;
import com.tchalanet.server.features.stats.aggregates.persistence.StatsEventLogJpaRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StatsAggregatesEventListener {

    private static final Logger log = LoggerFactory.getLogger(StatsAggregatesEventListener.class);

    private final StatsEventLogJpaRepository eventLogRepo;
    private final StatsDailyUpdaterService statsDailyUpdater;
    private final StatsDrawUpdaterService statsDrawUpdater;
    private final Clock clock;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTicketPlaced(TicketPlacedEvent event) {
        if (isOldEvent(event.occurredAt())) return;
        if (!markProcessedIfAbsent(event)) return;

        LocalDate refDate = LocalDate.ofInstant(event.occurredAt(), ZoneOffset.UTC);
        statsDailyUpdater.applyTicketPlaced(event, refDate);
        log.debug("Processed TicketPlacedEvent {}", event.eventId().value());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTicketCancelled(TicketCancelledEvent event) {
        if (isOldEvent(event.occurredAt())) return;
        if (!markProcessedIfAbsent(event)) return;

        LocalDate refDate = LocalDate.ofInstant(event.occurredAt(), ZoneOffset.UTC);
        statsDailyUpdater.applyTicketCancelled(event, refDate);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTicketSettled(TicketResultedEvent event) {
        if (isOldEvent(event.occurredAt())) return;
        if (!markProcessedIfAbsent(event)) return;

        LocalDate refDate = LocalDate.ofInstant(event.occurredAt(), ZoneOffset.UTC);
        statsDailyUpdater.applyTicketSettled(event, refDate);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSessionOpened(SalesSessionOpenedEvent event) {
        if (isOldEvent(event.occurredAt())) return;
        if (!markProcessedIfAbsent(event)) return;

        LocalDate refDate = LocalDate.ofInstant(event.occurredAt(), ZoneOffset.UTC);
        statsDailyUpdater.applySessionOpened(event, refDate);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSessionClosed(SalesSessionClosedEvent event) {
        if (isOldEvent(event.occurredAt())) return;
        if (!markProcessedIfAbsent(event)) return;

        var refDate = LocalDate.ofInstant(event.occurredAt(), ZoneOffset.UTC);
        statsDailyUpdater.applySessionClosed(event, refDate);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDrawResulted(DrawResultAppliedEvent event) {
        if (isOldEvent(event.occurredAt())) return;
        if (!markProcessedIfAbsent(event)) return;

        statsDrawUpdater.ensureDrawRow(event);
    }

    private UUID eventIdValue(Object eventId) {
        if (eventId == null) return null;
        if (eventId instanceof com.tchalanet.server.common.types.id.EventId eid) return eid.value();
        if (eventId instanceof UUID u) return u;
        throw new IllegalArgumentException("Unsupported eventId type: " + eventId.getClass());
    }

    private boolean isOldEvent(Instant occurredAt) {
        var eventDate = LocalDate.ofInstant(occurredAt, ZoneOffset.UTC);
        var today = LocalDate.now(clock);
        return eventDate.isBefore(today.minusDays(1));
    }

    private boolean markProcessedIfAbsent(DomainEvent event) {
        try {
            var e = new StatsEventLogEntity();
            e.setEventId(eventIdValue(event.eventId()));
            e.setEventType(event.getClass().getSimpleName());
            e.setProcessedAt(Instant.now(clock));
            eventLogRepo.save(e); // event_id unique
            return true;
        } catch (DataIntegrityViolationException dup) {
            return false;
        }
    }

}
