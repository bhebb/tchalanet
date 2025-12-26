package com.tchalanet.server.features.stats.aggregates.application;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.core.draw.domain.event.DrawResultedEvent;
import com.tchalanet.server.core.sales.domain.event.TicketCancelledEvent;
import com.tchalanet.server.core.sales.domain.event.TicketPlacedEvent;
import com.tchalanet.server.core.sales.domain.event.TicketResultedEvent;
import com.tchalanet.server.core.session.domain.event.SessionClosedEvent;
import com.tchalanet.server.core.session.domain.event.SessionOpenedEvent;
import com.tchalanet.server.features.stats.aggregates.persistence.StatsEventLogEntity;
import com.tchalanet.server.features.stats.aggregates.persistence.StatsEventLogJpaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class StatsAggregatesEventListener {

  private static final Logger log = LoggerFactory.getLogger(StatsAggregatesEventListener.class);

  private final StatsEventLogJpaRepository eventLogRepo;
  private final StatsDailyUpdater statsDailyUpdater;
  private final StatsDrawUpdater statsDrawUpdater;
  private final Clock clock;

  @EventListener
  @Transactional
  public void onTicketPlaced(TicketPlacedEvent event) {
    if (isOldEvent(event.occurredAt())) return;
    if (alreadyProcessed(event.eventId())) return;

    LocalDate refDate = LocalDate.ofInstant(event.occurredAt(), ZoneOffset.UTC);
    statsDailyUpdater.applyTicketPlaced(event, refDate);
    saveEventLog(event);
    log.debug("Processed TicketPlacedEvent {}", event.eventId());
  }

  @EventListener
  @Transactional
  public void onTicketCancelled(TicketCancelledEvent event) {
    if (isOldEvent(event.occurredAt())) return;
    if (alreadyProcessed(event.eventId())) return;

    LocalDate refDate = LocalDate.ofInstant(event.occurredAt(), ZoneOffset.UTC);
    statsDailyUpdater.applyTicketCancelled(event, refDate);
    saveEventLog(event);
  }

  @EventListener
  @Transactional
  public void onTicketSettled(TicketResultedEvent event) {
    if (isOldEvent(event.occurredAt())) return;
    if (alreadyProcessed(event.eventId())) return;

    LocalDate refDate = LocalDate.ofInstant(event.occurredAt(), ZoneOffset.UTC);
    statsDailyUpdater.applyTicketSettled(event, refDate);
    saveEventLog(event);
  }

  @EventListener
  @Transactional
  public void onSessionOpened(SessionOpenedEvent event) {
    if (isOldEvent(event.occurredAt())) return;
    if (alreadyProcessed(event.eventId())) return;

    LocalDate refDate = LocalDate.ofInstant(event.occurredAt(), ZoneOffset.UTC);
    statsDailyUpdater.applySessionOpened(event, refDate);
    saveEventLog(event);
  }

  @EventListener
  @Transactional
  public void onSessionClosed(SessionClosedEvent event) {
    if (isOldEvent(event.closedAt())) return;
    if (alreadyProcessed(event.eventId())) return;

    var refDate = LocalDate.ofInstant(event.closedAt(), ZoneOffset.UTC);
    statsDailyUpdater.applySessionClosed(event, refDate);
    saveEventLog(event);
  }

  @EventListener
  @Transactional
  public void onDrawResulted(DrawResultedEvent event) {
    if (isOldEvent(event.occurredAt())) return;
    if (alreadyProcessed(event.eventId())) return;

    statsDrawUpdater.ensureDrawRow(event);
    saveEventLog(event);
  }

  private boolean alreadyProcessed(UUID eventId) {
    return eventLogRepo.existsById(eventId);
  }

  private void saveEventLog(DomainEvent event) {
    var logEntity = new StatsEventLogEntity();
    logEntity.setEventId(event.eventId());
    logEntity.setEventType(event.getClass().getSimpleName());
    logEntity.setProcessedAt(Instant.now(clock));
    eventLogRepo.save(logEntity);
  }

  private boolean isOldEvent(Instant occurredAt) {
    var eventDate = LocalDate.ofInstant(occurredAt, ZoneOffset.UTC);
    var today = LocalDate.now(clock);
    return eventDate.isBefore(today.minusDays(1));
  }
}
