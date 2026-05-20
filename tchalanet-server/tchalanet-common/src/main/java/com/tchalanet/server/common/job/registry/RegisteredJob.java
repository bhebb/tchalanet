package com.tchalanet.server.common.job.registry;

import com.tchalanet.server.common.job.key.JobKey;
import java.util.Objects;
import java.util.Set;

/**
 * Public job metadata exposed to features/ops.
 *
 * <p>This record is intentionally Spring-free. It must not expose Spring Batch
 * implementation details such as bean names, Job, JobExecution, or JobParameters.
 */
public record RegisteredJob(
    JobKey jobKey,
    String displayName,
    JobScope scope,
    Set<String> requiredParams,
    Set<String> optionalParams
) {

    public RegisteredJob {
        Objects.requireNonNull(jobKey, "jobKey");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(scope, "scope");
        requiredParams = Set.copyOf(Objects.requireNonNull(requiredParams, "requiredParams"));
        optionalParams = Set.copyOf(Objects.requireNonNull(optionalParams, "optionalParams"));
    }

    public enum JobScope {
        TENANT,
        GLOBAL
    }
}
