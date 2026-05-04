package com.tchalanet.server.core.draw.application.query.model;

import com.tchalanet.server.common.types.id.ResultSlotId;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

public record DrawSearchCriteria(
    ResultSlotId resultSlotId,
    String status,
    LocalDate from,
    LocalDate to,
    int days,
    Integer limitPerChannel,
    Integer lookaheadHours,
    List<String> resultSlotKeys
) {

    public static DrawSearchCriteria today(ResultSlotId resultSlotId, Clock clock) {
        var today = LocalDate.now(clock);
        return new DrawSearchCriteria(resultSlotId, null, today, today, 1, null, null, null);
    }

    public static DrawSearchCriteria lastDays(ResultSlotId resultSlotId, int days, Clock clock) {
        var to = LocalDate.now(clock);
        var from = to.minusDays(days);
        return new DrawSearchCriteria(resultSlotId, null, from, to, days, null, null, null);
    }

    public static DrawSearchCriteria upcoming(ResultSlotId resultSlotId, int days, Clock clock) {
        var from = LocalDate.now(clock);
        var to = from.plusDays(days);
        return new DrawSearchCriteria(resultSlotId, null, from, to, days, null, null, null);
    }

    public static DrawSearchCriteria of(ResultSlotId resultSlotId, LocalDate from, LocalDate to, Clock clock) {
        return of(resultSlotId, null, from, to, clock);
    }

    public static DrawSearchCriteria of(ResultSlotId resultSlotId, String status, LocalDate from, LocalDate to, Clock clock) {
        // Protect against null dates; use today as default
        var now = LocalDate.now(clock);
        var f = from == null ? now : from;
        var t = to == null ? now : to;
        // normalize so from <= to
        if (f.isAfter(t)) {
            LocalDate tmp = f;
            f = t;
            t = tmp;
        }
        // compute inclusive days (same-day => 1)
        int computedDays = (int) (t.toEpochDay() - f.toEpochDay()) + 1;
        return new DrawSearchCriteria(resultSlotId, status, f, t, computedDays, null, null, null);
    }

    public static DrawSearchCriteria forNext(ResultSlotId resultSlotId, int lookaheadHours, int limitPerChannel) {
        return new DrawSearchCriteria(resultSlotId, null, null, null, 0, limitPerChannel, lookaheadHours, null);
    }

    public static DrawSearchCriteria forLatestWithResults(List<String> resultSlotKeys) {
        return new DrawSearchCriteria(null, null, null, null, 0, null, null, resultSlotKeys);
    }
}
