package com.tchalanet.server.common.job.lifecycle;

import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;
import java.util.Map;

public record JobLifecycleEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    String requestId,
    String jobKey,
    JobLifecycleStatus status,
    String code,
    String message,
    Map<String, Object> details
) {}
