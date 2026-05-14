package com.tchalanet.server.common.job.model;

import com.tchalanet.server.common.job.key.JobKey;
import java.time.Instant;

public record BatchJobStartResult(
    JobKey jobKey,
    String executionId,
    BatchJobStartStatus status,
    Instant startedAt
) {}
