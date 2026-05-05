package com.tchalanet.server.core.draw.infra.persistence.mapper;

import com.tchalanet.server.common.types.id.*;
import com.tchalanet.server.core.draw.application.query.projection.DrawResultSummary;
import com.tchalanet.server.core.draw.application.query.projection.DrawSummary;
import com.tchalanet.server.core.draw.infra.persistence.view.DrawSummaryViewEntity;
import com.tchalanet.server.core.drawresult.domain.model.DrawResultStatus;
import org.springframework.stereotype.Component;

@Component
public class DrawSummaryViewMapper {

    public DrawSummary toProjection(DrawSummaryViewEntity v) {
        if (v == null) {
            return null;
        }

        var result = v.getDrawResultId() == null
            ? null
            : new DrawResultSummary(
                DrawResultId.of(v.getDrawResultId()),
            DrawResultStatus.valueOf(v.getDrawResultStatus()),
                v.getDrawResultOccurredAt(),
                v.getSourceHash(),
                v.getHaitiResult()
            );

        return new DrawSummary(
            DrawId.of(v.getDrawId()),
            TenantId.of(v.getTenantId()),
            v.getDrawDate(),
            v.getStatus(),
            v.getScheduledAt(),
            v.getOpenedAt(),
            v.getClosedAt(),
            v.getCutoffAt(),
            v.getResultedAt(),
            v.getSettledAt(),
            DrawChannelId.of(v.getDrawChannelId()),
            v.getDrawChannelCode(),
            v.getDrawChannelLabel(),
            v.getDrawTime(),
            v.getDrawTimezone(),
            Boolean.TRUE.equals(v.getDrawChannelActive()),
            ResultSlotId.of(v.getResultSlotId()),
            v.getResultSlotKey(),
            v.getResultProvider(),
            v.getResultTimezone(),
            v.getResultDrawTime(),
            Boolean.TRUE.equals(v.getResultActive()),
            result
        );
    }
}
