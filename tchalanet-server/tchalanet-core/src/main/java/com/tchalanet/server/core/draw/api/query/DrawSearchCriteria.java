package com.tchalanet.server.core.draw.api.query;

import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.core.draw.internal.domain.model.DrawStatus;
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
        LocalDate to) {

        return new DrawSearchCriteria(
            resultSlotId,
            parseStatus(status),
            from,
            to,
            null,
            null,
            null);
    }

    public static DrawSearchCriteria today(ResultSlotId resultSlotId, LocalDate today) {
        return new DrawSearchCriteria(
            resultSlotId,
            null,
            today,
            today,
            null,
            null,
            null);
    }

    public static DrawSearchCriteria upcoming(
        ResultSlotId resultSlotId,
        LocalDate today,
        int days) {

        return new DrawSearchCriteria(
            resultSlotId,
            null,
            today,
            today.plusDays(days),
            null,
            null,
            null);
    }

    public static DrawSearchCriteria forNext(
        ResultSlotId resultSlotId,
        int lookaheadHours,
        int limitPerChannel) {

        return new DrawSearchCriteria(
            resultSlotId,
            null,
            null,
            null,
            limitPerChannel,
            lookaheadHours,
            null);
    }

    public static DrawSearchCriteria forLatestWithResults(List<String> resultSlotKeys) {
        return new DrawSearchCriteria(
            null,
            null,
            null,
            null,
            null,
            null,
            normalizeKeys(resultSlotKeys));
    }

    public static DrawSearchCriteria forResults(
        List<String> resultSlotKeys,
        LocalDate from,
        LocalDate to) {

        return new DrawSearchCriteria(
            null,
            DrawStatus.RESULTED,
            from,
            to,
            null,
            null,
            normalizeKeys(resultSlotKeys));
    }

    private static DrawStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        return DrawStatus.valueOf(status.trim().toUpperCase());
    }

    private static List<String> normalizeKeys(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return null;
        }

        var normalized =
            keys.stream()
                .filter(k -> k != null && !k.isBlank())
                .map(k -> k.trim().toUpperCase())
                .distinct()
                .toList();

        return normalized.isEmpty() ? null : normalized;
    }
}
