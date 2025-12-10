package com.tchalanet.features.stats.aggregates.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class StatsRepositoriesDataJpaTest {

    @Autowired
    private StatsDailyJpaRepository statsDailyJpaRepository;

    @Autowired
    private StatsDrawJpaRepository statsDrawJpaRepository;

    @Autowired
    private StatsEventLogJpaRepository statsEventLogJpaRepository;

    @Test
    void statsDaily_crud_and_query() {
        UUID dimId = UUID.randomUUID();
        StatsDailyEntity entity = StatsDailyEntity.builder()
                .dimensionType("tenant")
                .dimensionId(dimId)
                .refDate(LocalDate.now())
                .ticketsCount(10)
                .ticketsCancelledCount(1)
                .stakeSumCents(1000)
                .winningsSumCents(200)
                .netRevenueCents(800)
                .payoutsCount(1)
                .sessionsOpenedCount(2)
                .sessionsClosedCount(1)
                .version(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        StatsDailyEntity saved = statsDailyJpaRepository.save(entity);
        Optional<StatsDailyEntity> found = statsDailyJpaRepository.findByDimensionTypeAndDimensionIdAndRefDate("tenant", dimId, entity.getRefDate());
        assertThat(found).isPresent();
        assertThat(found.get().getTicketsCount()).isEqualTo(10);

        statsDailyJpaRepository.deleteByRefDateBetween(entity.getRefDate(), entity.getRefDate());
        assertThat(statsDailyJpaRepository.findByDimensionTypeAndDimensionIdAndRefDate("tenant", dimId, entity.getRefDate())).isEmpty();
    }

    @Test
    void statsDraw_and_event_log_basic() {
        StatsDrawEntity draw = StatsDrawEntity.builder()
                .drawId(UUID.randomUUID())
                .tenantId(UUID.randomUUID())
                .gameCode("GAME1")
                .scheduledAt(Instant.now())
                .ticketsCount(5)
                .stakeSumCents(500)
                .winningsSumCents(100)
                .netRevenueCents(400)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        StatsDrawEntity saved = statsDrawJpaRepository.save(draw);
        assertThat(statsDrawJpaRepository.findByDrawId(saved.getDrawId())).isNotEmpty();

        StatsEventLogEntity ev = StatsEventLogEntity.builder()
                .eventId(UUID.randomUUID())
                .eventType("TEST")
                .processedAt(Instant.now())
                .build();

        statsEventLogJpaRepository.save(ev);
        assertThat(statsEventLogJpaRepository.findByEventType("TEST")).isNotEmpty();
    }
}

