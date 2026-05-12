package com.tchalanet.server.core.draw.internal.infra.persistence.mapper;

import com.tchalanet.server.common.types.id.*;
import com.tchalanet.server.core.draw.application.query.projection.DrawResultSummary;
import com.tchalanet.server.core.draw.application.query.projection.DrawSummary;
import com.tchalanet.server.core.draw.infra.persistence.view.DrawSummaryViewEntity;
import com.tchalanet.server.core.drawresult.domain.model.DrawResultStatus;
import org.springframework.stereotype.Component;

@Component
public class DrawSummaryViewMapper {

    public DrawSummary toProjection(DrawSummaryViewEntity entity) {
        if (entity == null) {
            return null;
        }

        var result = entity.getDrawResultId() == null
            ? null
            : new DrawResultSummary(
                DrawResultId.of(entity.getDrawResultId()),
            DrawResultStatus.valueOf(entity.getDrawResultStatus()),
                entity.getDrawResultOccurredAt(),
                entity.getSourceHash(),
                entity.getHaitiResult()
            );

        return new DrawSummary(
            DrawId.of(entity.getDrawId()),
            TenantId.of(entity.getTenantId()),
            entity.getDrawDate(),
            entity.getStatus(),
            entity.getScheduledAt(),
            entity.getOpenedAt(),
            entity.getClosedAt(),
            entity.getCutoffAt(),
            entity.getResultedAt(),
            entity.getSettledAt(),
            DrawChannelId.of(entity.getDrawChannelId()),
            entity.getDrawChannelCode(),
            entity.getDrawChannelLabel(),
            entity.getDrawTime(),
            entity.getDrawTimezone(),
            Boolean.TRUE.equals(entity.getDrawChannelActive()),
            ResultSlotId.of(entity.getResultSlotId()),
            entity.getResultSlotKey(),
            entity.getResultProvider(),
            entity.getResultTimezone(),
            entity.getResultDrawTime(),
            Boolean.TRUE.equals(entity.getResultActive()),
            result
        );
    }
}
