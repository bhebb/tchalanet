package com.tchalanet.server.common.batch.registry;

import com.tchalanet.server.common.batch.key.JobKey;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a registered batch job in the allowlist.
 *
 * Contains metadata about scope, required/optional params, and Spring bean name.
 */
public record RegisteredJob(
    JobKey jobKey,
    String displayName,
    JobScope scope,
    Set<String> requiredParams,
    Set<String> optionalParams,
    String springJobBeanName
) {
    public RegisteredJob {
        Objects.requireNonNull(jobKey, "jobKey");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(requiredParams, "requiredParams");
        Objects.requireNonNull(optionalParams, "optionalParams");
        Objects.requireNonNull(springJobBeanName, "springJobBeanName");
    }

    /**
     * Job scope determines whether tenant context is required.
     */
    public enum JobScope {
        /**
         * Job operates within a tenant context (requires tenant_id param).
         */
        TENANT,

        /**
         * Job operates globally, no tenant context (tenant_id forbidden).
         */
        GLOBAL
    }
}
