package com.tchalanet.server.core.draw.internal.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.catalog.drawchannel.api.model.DrawSource;
import com.tchalanet.server.core.draw.api.model.DrawStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
    name = "draw",
    indexes = {
        @Index(name = "ix_draw_tenant_date", columnList = "tenant_id, draw_date"),
        @Index(name = "ix_draw_tenant_scheduled", columnList = "tenant_id, scheduled_at"),
        @Index(name = "ix_draw_status_scheduled_at", columnList = "status, scheduled_at"),
        @Index(name = "ix_draw_status_cutoff_at", columnList = "status, cutoff_at"),
        @Index(name = "ix_draw_result_id", columnList = "draw_result_id")
    })
@Audited
@Getter
@Setter
public class DrawJpaEntity extends BaseTenantEntity {

    @Column(name = "draw_channel_id", nullable = false)
    private UUID drawChannelId;

    @Column(name = "draw_date", nullable = false)
    private LocalDate drawDate;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "cutoff_at", nullable = false)
    private Instant cutoffAt;

    @Column(name = "opened_at")
    private Instant openedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "resulted_at")
    private Instant resultedAt;

    @Column(name = "settled_at")
    private Instant settledAt;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    @Column(name = "cancel_reason_code", length = 96)
    private String cancelReasonCode;

    @Column(name = "cancel_reason_label", length = 255)
    private String cancelReasonLabel;

    @Column(name = "status", nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private DrawStatus status;

    @Column(name = "draw_result_id")
    private UUID drawResultId;

    @Column(name = "system_generated", nullable = false)
    private boolean systemGenerated = true;

    @Column(name = "locked", nullable = false)
    private boolean locked = false;

    @Column(name = "result_source", length = 16)
    @Enumerated(EnumType.STRING)
    private DrawSource resultSource;

    @Column(name = "result_override_reason")
    private String resultOverrideReason;

    @Column(name = "result_overridden_at")
    private Instant resultOverriddenAt;
}
