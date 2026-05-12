package com.tchalanet.server.core.draw.internal.infra.persistence.view;

import com.tchalanet.server.core.draw.internal.domain.model.DrawStatus;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.JdbcTypeCode;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "v_draw_summary")
@Getter
public class DrawSummaryViewEntity {

    @Id
    @Column(name = "draw_id")
    private UUID drawId;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "draw_date")
    private LocalDate drawDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private DrawStatus status;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "opened_at")
    private Instant openedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "cutoff_at")
    private Instant cutoffAt;

    @Column(name = "resulted_at")
    private Instant resultedAt;

    @Column(name = "settled_at")
    private Instant settledAt;

    @Column(name = "draw_channel_id")
    private UUID drawChannelId;

    @Column(name = "draw_channel_code")
    private String drawChannelCode;

    @Column(name = "draw_channel_label")
    private String drawChannelLabel;

    @Column(name = "draw_time")
    private LocalTime drawTime;

    @Column(name = "draw_timezone")
    private String drawTimezone;

    @Column(name = "draw_channel_active")
    private Boolean drawChannelActive;

    @Column(name = "result_slot_id")
    private UUID resultSlotId;

    @Column(name = "result_slot_key")
    private String resultSlotKey;

    @Column(name = "result_provider")
    private String resultProvider;

    @Column(name = "result_timezone")
    private String resultTimezone;

    @Column(name = "result_draw_time")
    private LocalTime resultDrawTime;

    @Column(name = "result_active")
    private Boolean resultActive;

    @Column(name = "draw_result_id")
    private UUID drawResultId;

    @Column(name = "draw_result_status")
    private String drawResultStatus;

    @Column(name = "draw_result_occurred_at")
    private Instant drawResultOccurredAt;

    @Column(name = "source_hash")
    private String sourceHash;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "haiti_result", columnDefinition = "jsonb")
    private Map<String, Object> haitiResult;
}
