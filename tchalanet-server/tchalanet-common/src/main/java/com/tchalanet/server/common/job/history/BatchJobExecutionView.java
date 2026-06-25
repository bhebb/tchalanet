package com.tchalanet.server.common.job.history;

import java.time.Instant;

public record BatchJobExecutionView(
    long executionId,
    String jobKey,
    String jobName,
    String status,
    Instant startedAt,
    Instant endedAt,
    String context
) {}
