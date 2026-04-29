package com.tchalanet.server.features.stats.aggregates;

import com.tchalanet.server.core.draw.domain.event.DrawResultAppliedEvent;
import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.core.sales.domain.event.TicketCancelledEvent;
import com.tchalanet.server.core.sales.domain.event.TicketPlacedEvent;
import com.tchalanet.server.core.sales.domain.event.TicketResultedEvent;
import com.tchalanet.server.core.session.domain.event.SessionClosedEvent;
import com.tchalanet.server.core.session.domain.event.SessionOpenedEvent;
import com.tchalanet.server.features.stats.aggregates.app.StatsDailyUpdaterService;
import com.tchalanet.server.features.stats.aggregates.app.StatsDrawUpdaterService;
import com.tchalanet.server.features.stats.aggregates.persistence.StatsEventLogEntity;
import com.tchalanet.server.features.stats.aggregates.persistence.StatsEventLogJpaRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
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
        if (alreadyProcessed(event.eventId().value())) return;

        LocalDate refDate = LocalDate.ofInstant(event.occurredAt(), ZoneOffset.UTC);
        statsDailyUpdater.applyTicketPlaced(event, refDate);
        markProcessedIfAbsent(event);
        log.debug("Processed TicketPlacedEvent {}", event.eventId().value());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void onTicketCancelled(TicketCancelledEvent event) {
        if (isOldEvent(event.occurredAt())) return;
        if (alreadyProcessed(event.eventId().value())) return;

        LocalDate refDate = LocalDate.ofInstant(event.occurredAt(), ZoneOffset.UTC);
        statsDailyUpdater.applyTicketCancelled(event, refDate);
        markProcessedIfAbsent(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void onTicketSettled(TicketResultedEvent event) {
        if (isOldEvent(event.occurredAt())) return;
        if (alreadyProcessed(event.eventId().value())) return;

        LocalDate refDate = LocalDate.ofInstant(event.occurredAt(), ZoneOffset.UTC);
        statsDailyUpdater.applyTicketSettled(event, refDate);
        markProcessedIfAbsent(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void onSessionOpened(SessionOpenedEvent event) {
        if (isOldEvent(event.occurredAt())) return;
        if (alreadyProcessed(event.eventId().value())) return;

        LocalDate refDate = LocalDate.ofInstant(event.occurredAt(), ZoneOffset.UTC);
        statsDailyUpdater.applySessionOpened(event, refDate);
        markProcessedIfAbsent(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void onSessionClosed(SessionClosedEvent event) {
        if (isOldEvent(event.closedAt())) return;
        if (alreadyProcessed(event.eventId().value())) return;

        var refDate = LocalDate.ofInstant(event.closedAt(), ZoneOffset.UTC);
        statsDailyUpdater.applySessionClosed(event, refDate);
        markProcessedIfAbsent(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDrawResulted(DrawResultAppliedEvent event) {
        if (isOldEvent(event.occurredAt())) return;
        if (alreadyProcessed(event.eventId().value())) return;

        statsDrawUpdater.ensureDrawRow(event);
        markProcessedIfAbsent(event);
    }

    private boolean alreadyProcessed(Object eventId) {
        if (eventId == null) return false;
        if (eventId instanceof com.tchalanet.server.common.types.id.EventId eid) {
            return eventLogRepo.existsById(eid.value());
        }
        if (eventId instanceof UUID u) {
            return eventLogRepo.existsById(u);
        }
        throw new IllegalArgumentException("Unsupported eventId type: " + eventId.getClass());
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
