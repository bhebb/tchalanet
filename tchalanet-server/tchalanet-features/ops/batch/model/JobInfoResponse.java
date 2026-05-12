package com.tchalanet.server.features.ops.batch.model;

import java.util.Set;

/**
 * Response containing job metadata.
 */
public record JobInfoResponse(
    String job_key,
    String display_name,
    String scope,
    Set<String> required_params,
    Set<String> optional_params
) {}
