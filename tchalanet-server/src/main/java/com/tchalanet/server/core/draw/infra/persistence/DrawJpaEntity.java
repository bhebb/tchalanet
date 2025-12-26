package com.tchalanet.server.core.draw.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.core.draw.domain.model.DrawStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.Instant;

@Entity
@Table(name = "draw",
    indexes = {
        @Index(name = "ix_draw_tenant_scheduled", columnList = "tenant_id, scheduled_at"),
        @Index(name = "ix_draw_status_sched", columnList = "status, scheduled_at")
    }
)
@Audited
@Getter
@Setter
@NoArgsConstructor
public class DrawJpaEntity extends BaseTenantEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "draw_channel_id", nullable = false)
    private DrawChannelJpaEntity drawChannel;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "cutoff_sec", nullable = false)
    private Integer cutoffSec;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private DrawStatus status;

    @Column(name = "draw_source")
    private String drawSource;

    @Column(name = "system_generated", nullable = false)
    private Boolean systemGenerated = Boolean.TRUE;

    @Column(name = "locked", nullable = false)
    private Boolean locked = Boolean.FALSE;
}
