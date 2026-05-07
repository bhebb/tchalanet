package com.tchalanet.server.core.audit.application.command.model;

import java.time.Instant;

public record PurgeOldAuditEventsResult(int deleted, int retentionDays, Instant threshold) {}
