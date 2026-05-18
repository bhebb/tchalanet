package com.tchalanet.server.core.drawresult.internal.application.port.out.external.notification;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface DrawResultFetchNotificationPort {
    void notifyFetched(DrawResultFetchNotification notification);

    record DrawResultFetchNotification(
        String provider,
        String slotKey,
        LocalDate drawDate,
        Instant occurredAt,
        String status,
        String quality,
        int itemCount,
        List<String> externalGameCodes,
        Map<String, String> metadata
    ) {}
}
