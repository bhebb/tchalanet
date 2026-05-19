package com.tchalanet.server.core.drawresult.internal.application.port.out.notification;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface DrawResultFetchNotificationPort {

    void notifyFetched(DrawResultFetchNotification notification);

    default void notifyFetchedBatch(List<DrawResultFetchNotification> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            return;
        }

        notifications.forEach(this::notifyFetched);
    }

    default void notifyFetchFailedBatch(DrawResultFetchFailureBatchNotification notification) {
        // no-op by default for adapters that only support success notifications
    }

    record DrawResultFetchNotification(
        String provider,
        String slotKey,
        LocalDate resultDate,
        Instant occurredAt,
        String status,
        String quality,
        int externalCount,
        List<String> externalGames,
        Map<String, String> metadata) {}

    record DrawResultFetchFailure(
        String provider,
        String slotKey,
        LocalDate resultDate,
        Instant expectedOccurredAt,
        String errorType,
        String message) {}

    record DrawResultFetchFailureBatchNotification(
        LocalDate baseDate,
        int daysBack,
        int totalFailures,
        List<DrawResultFetchFailure> failures) {}
}
