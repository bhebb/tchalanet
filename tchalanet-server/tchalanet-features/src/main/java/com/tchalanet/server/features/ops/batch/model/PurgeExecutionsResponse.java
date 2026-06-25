package com.tchalanet.server.features.ops.batch.model;

import java.time.Instant;

public record PurgeExecutionsResponse(
    Instant cutoff,
    int job_execution_context_rows,
    int step_execution_context_rows,
    int step_execution_rows,
    int job_execution_param_rows,
    int job_execution_rows,
    int job_instance_rows
) {}
