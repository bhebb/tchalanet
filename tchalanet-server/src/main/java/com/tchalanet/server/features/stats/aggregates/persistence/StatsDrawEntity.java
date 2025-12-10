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
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stats_draw")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsDrawEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "draw_id", nullable = false, unique = true, columnDefinition = "uuid")
    private UUID drawId;

    @Column(name = "tenant_id", nullable = false, columnDefinition = "uuid")
    private UUID tenantId;

    @Column(name = "game_code", nullable = false)
    private String gameCode;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "tickets_count", nullable = false)
    private long ticketsCount;

    @Column(name = "stake_sum_cents", nullable = false)
    private long stakeSumCents;

    @Column(name = "winnings_sum_cents", nullable = false)
    private long winningsSumCents;

    @Column(name = "net_revenue_cents", nullable = false)
    private long netRevenueCents;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
