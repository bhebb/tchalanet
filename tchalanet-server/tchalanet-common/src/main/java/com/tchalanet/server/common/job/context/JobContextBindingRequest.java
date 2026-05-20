package com.tchalanet.server.common.job.context;

import java.util.Map;

public record JobContextBindingRequest(
    JobExecutionScope scope,
    Map<String, String> params
) {

    public static JobContextBindingRequest tenant(Map<String, String> params) {
        return new JobContextBindingRequest(JobExecutionScope.TENANT, params);
    }

    public static JobContextBindingRequest platform(Map<String, String> params) {
        return new JobContextBindingRequest(JobExecutionScope.PLATFORM, params);
    }
}
