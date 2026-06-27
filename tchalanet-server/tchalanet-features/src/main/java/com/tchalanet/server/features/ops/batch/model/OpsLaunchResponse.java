package com.tchalanet.server.features.ops.batch.model;

import java.util.List;

public record OpsLaunchResponse(
    String job_key,
    int requested,
    int started,
    int failed,
    List<OpsJobLaunchItem> launches,
    String message
) {}
