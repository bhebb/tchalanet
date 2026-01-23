package com.tchalanet.server.features.ops.dto;

import java.util.Map;

/**
 * Request to start a batch job.
 *
 * params: Map of job-specific parameters (snake_case keys)
 */
public record StartJobRequest(
    Map<String, String> params
) {
    public StartJobRequest {
        if (params == null) {
            params = Map.of();
        }
    }
}
