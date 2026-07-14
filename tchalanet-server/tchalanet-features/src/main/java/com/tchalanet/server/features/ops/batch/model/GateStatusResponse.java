package com.tchalanet.server.features.ops.batch.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Effective gate status for one batch job — same JSON keys as the previous raw map. */
public record GateStatusResponse(
    @JsonProperty("job_key") String jobKey,
    boolean enabled,
    String scope,
    @JsonProperty("tenant_id") String tenantId) {}
