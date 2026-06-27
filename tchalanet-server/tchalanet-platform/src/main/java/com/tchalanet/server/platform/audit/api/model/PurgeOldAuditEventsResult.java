package com.tchalanet.server.platform.audit.api.model;

import java.time.Instant;

public record PurgeOldAuditEventsResult(int deleted, int retentionDays, Instant threshold, String reason) {}
