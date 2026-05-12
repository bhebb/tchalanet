package com.tchalanet.server.features.ops.batch.model;

import java.time.Instant;

/**
 * Response after starting a batch job.
 */
public record StartJobResponse(
    String job_key,
    long execution_id,
    String status,
    Instant started_at
) {}
