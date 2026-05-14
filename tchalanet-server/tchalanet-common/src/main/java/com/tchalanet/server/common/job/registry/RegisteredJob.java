package com.tchalanet.server.common.job.registry;

import com.tchalanet.server.common.job.key.JobKey;
import java.util.Set;

public record RegisteredJob(
    JobKey jobKey,
    String displayName,
    JobScope scope,
    Set<String> requiredParams,
    Set<String> optionalParams,
    String springJobBeanName
) {

    public enum JobScope {
        GLOBAL,
        TENANT
    }
}
