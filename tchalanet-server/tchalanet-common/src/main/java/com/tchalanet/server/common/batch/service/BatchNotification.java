package com.tchalanet.server.common.batch.service;

import java.time.Instant;
import java.util.Map;

public record BatchNotification(
    String jobKey,
    String tenantId,
    BatchNotificationStatus status,
    String code,
    String message,
    String requestId,
    Instant occurredAt,
    Map<String, Object> details
) {
}
