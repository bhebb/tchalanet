package com.tchalanet.server.features.stats.aggregates.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "stats_daily")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsDailyEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "dimension_type", nullable = false)
    private String dimensionType;

    @Column(name = "dimension_id", columnDefinition = "uuid")
    private UUID dimensionId;

    @Column(name = "ref_date", nullable = false)
    private LocalDate refDate;

    @Column(name = "tickets_count", nullable = false)
    private long ticketsCount;

    @Column(name = "tickets_cancelled_count", nullable = false)
    private long ticketsCancelledCount;

    @Column(name = "stake_sum_cents", nullable = false)
    private long stakeSumCents;

    @Column(name = "winnings_sum_cents", nullable = false)
    private long winningsSumCents;

    @Column(name = "net_revenue_cents", nullable = false)
    private long netRevenueCents;

    @Column(name = "payouts_count", nullable = false)
    private long payoutsCount;

    @Column(name = "sessions_opened_count", nullable = false)
    private long sessionsOpenedCount;

    @Column(name = "sessions_closed_count", nullable = false)
    private long sessionsClosedCount;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

