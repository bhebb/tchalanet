package com.tchalanet.server.core.draw.infra.persistence.mapper;

import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.domain.model.Draw;
import com.tchalanet.server.core.draw.domain.model.DrawStatus;
import com.tchalanet.server.core.draw.infra.persistence.DrawJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class DrawMapper {

    public Draw toDomain(DrawJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        var status = entity.getStatus() == null
            ? DrawStatus.SCHEDULED
            : entity.getStatus();

        var drawResultId = entity.getDrawResultId() == null
            ? null
            : DrawResultId.of(entity.getDrawResultId());

        return new Draw(
            DrawId.of(entity.getId()),
            TenantId.of(entity.getTenantId()),
            DrawChannelId.of(entity.getDrawChannelId()),
            entity.getDrawDate(),
            entity.getScheduledAt(),
            entity.getCutoffAt(),
            status,
            drawResultId,
            entity.getOpenedAt(),
            entity.getClosedAt(),
            entity.getResultedAt(),
            entity.getSettledAt(),
            entity.getCanceledAt(),
            entity.getCancelReason(),
            entity.getResultSource(),
            entity.getResultOverrideReason(),
            entity.getResultOverriddenAt(),
            entity.isLocked(),
            entity.isSystemGenerated()
        );
    }

    public DrawJpaEntity toEntity(Draw domain) {
        if (domain == null) {
            return null;
        }

        var entity = new DrawJpaEntity();

        entity.setId(domain.id().value());
        entity.setTenantId(domain.tenantId().value());
        entity.setDrawChannelId(domain.drawChannelId().value());

        entity.setDrawDate(domain.drawDate());
        entity.setScheduledAt(domain.scheduledAt());
        entity.setCutoffAt(domain.cutoffAt());

        entity.setStatus(domain.status());

        entity.setDrawResultId(
            domain.drawResultId() == null ? null : domain.drawResultId().value()
        );

        entity.setOpenedAt(domain.openedAt());
        entity.setClosedAt(domain.closedAt());
        entity.setResultedAt(domain.resultedAt());
        entity.setSettledAt(domain.settledAt());
        entity.setCanceledAt(domain.canceledAt());
        entity.setCancelReason(domain.cancelReason());

        entity.setResultSource(domain.resultSource());
        entity.setResultOverrideReason(domain.resultOverrideReason());
        entity.setResultOverriddenAt(domain.resultOverriddenAt());

        entity.setLocked(domain.locked());
        entity.setSystemGenerated(domain.systemGenerated());

        return entity;
    }
}
