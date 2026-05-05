package com.tchalanet.server.core.draw.application.query.model;

import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.core.draw.domain.model.DrawStatus;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

public record DrawSearchCriteria(
    ResultSlotId resultSlotId,
    DrawStatus status,
    LocalDate from,
    LocalDate to,
    Integer limitPerChannel,
    Integer lookaheadHours,
    List<String> resultSlotKeys
) {

    public static DrawSearchCriteria of(
        ResultSlotId resultSlotId,
        String status,
        LocalDate from,
        LocalDate to
    ) {
        DrawStatus drawStatus = status == null || status.isBlank()
            ? null
            : DrawStatus.valueOf(status.toUpperCase());

        return new DrawSearchCriteria(
            resultSlotId,
            drawStatus,
            from,
            to,
            null,
            null,
            null
        );
    }

    public static DrawSearchCriteria today(ResultSlotId resultSlotId, LocalDate today) {
        return new DrawSearchCriteria(
            resultSlotId,
            null,
            today,
            today,
            null,
            null,
            null
        );
    }

    public static DrawSearchCriteria upcoming(ResultSlotId resultSlotId, int days, Clock clock) {
        LocalDate today = LocalDate.now(clock);
        LocalDate until = today.plusDays(days);
        return new DrawSearchCriteria(
            resultSlotId,
            null,
            today,
            until,
            null,
            null,
            null
        );
    }

    public static DrawSearchCriteria forNext(
        ResultSlotId resultSlotId,
        int lookaheadHours,
        int limitPerChannel
    ) {
        return new DrawSearchCriteria(
            resultSlotId,
            null,
            null,
            null,
            limitPerChannel,
            lookaheadHours,
            null
        );
    }

    public static DrawSearchCriteria forLatestWithResults(
        List<String> resultSlotKeys
    ) {
        return new DrawSearchCriteria(
            null,
            null,
            null,
            null,
            null,
            null,
            resultSlotKeys
        );
    }

}
