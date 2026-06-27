package com.tchalanet.server.features.ops.batch.model;

public record OpsJobLaunchItem(
    String tenant_id,
    Long execution_id,
    String status,
    String error
) {}
