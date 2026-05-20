package com.tchalanet.server.features.ops.batch.model;

import java.time.Instant;

/**
 * Response containing job execution details.
 */
public record ExecutionResponse(
    long execution_id,
    String job_key,
    String status,
    Instant started_at,
    Instant ended_at
) {}
