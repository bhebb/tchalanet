package com.tchalanet.server.common.job.history;

import java.time.Instant;

public record BatchJobHistoryPurgeResult(
    Instant cutoff,
    int jobExecutionContextRows,
    int stepExecutionContextRows,
    int stepExecutionRows,
    int jobExecutionParamRows,
    int jobExecutionRows,
    int jobInstanceRows
) {}
