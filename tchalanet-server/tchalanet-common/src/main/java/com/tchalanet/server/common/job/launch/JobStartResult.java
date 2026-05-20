package com.tchalanet.server.common.job.launch;

public record JobStartResult(String jobInstanceId, String jobExecutionId, String status) {}
