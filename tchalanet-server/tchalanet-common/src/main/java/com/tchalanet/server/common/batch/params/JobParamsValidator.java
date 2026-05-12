package com.tchalanet.server.common.batch.params;

import com.tchalanet.server.common.batch.key.JobKey;
import com.tchalanet.server.common.batch.registry.RegisteredJob;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates batch job parameters against registered job metadata.
 *
 * Checks:
 * - required params are present
 * - no unknown params
 * - tenant params forbidden on GLOBAL jobs
 */
@Component
public class JobParamsValidator {

    private static final Set<String> TENANT_PARAMS = Set.of(
        BatchParamKeys.TENANT_ID,
        BatchParamKeys.TENANT_CODE,
        BatchParamKeys.TENANT_ZONE_ID,
        BatchParamKeys.TENANT_CURRENCY
    );

    /**
     * Validate parameters for a job.
     *
     * @param jobKey the job key (for error messages)
     * @param registeredJob the registered job metadata
     * @param params the parameters to validate
     * @throws IllegalArgumentException if validation fails
     */
    public void validate(JobKey jobKey, RegisteredJob registeredJob, Map<String, String> params) {
        Set<String> providedKeys = params.keySet();
        Set<String> requiredKeys = registeredJob.requiredParams();
        Set<String> optionalKeys = registeredJob.optionalParams();
        RegisteredJob.JobScope scope = registeredJob.scope();

        // Check: required params present
        Set<String> missingRequired = new HashSet<>(requiredKeys);
        missingRequired.removeAll(providedKeys);

        if (!missingRequired.isEmpty()) {
            throw new IllegalArgumentException(
                String.format("Missing required parameters for job %s: %s",
                    jobKey, missingRequired));
        }

        // Check: unknown params
        Set<String> allowedKeys = new HashSet<>();
        allowedKeys.addAll(requiredKeys);
        allowedKeys.addAll(optionalKeys);
        allowedKeys.add(BatchParamKeys.TS); // always allowed (technical)

        Set<String> unknownKeys = providedKeys.stream()
            .filter(k -> !allowedKeys.contains(k))
            .collect(Collectors.toSet());

        if (!unknownKeys.isEmpty()) {
            throw new IllegalArgumentException(
                String.format("Unknown parameters for job %s: %s",
                    jobKey, unknownKeys));
        }

        // Check: tenant params on GLOBAL jobs
        if (scope == RegisteredJob.JobScope.GLOBAL) {
            Set<String> forbiddenTenantParams = providedKeys.stream()
                .filter(TENANT_PARAMS::contains)
                .collect(Collectors.toSet());

            if (!forbiddenTenantParams.isEmpty()) {
                throw new IllegalArgumentException(
                    String.format("Tenant parameters not allowed for GLOBAL job %s: %s",
                        jobKey, forbiddenTenantParams));
            }
        }

        // Check: tenant_id required for TENANT jobs
        if (scope == RegisteredJob.JobScope.TENANT) {
            if (!providedKeys.contains(BatchParamKeys.TENANT_ID)) {
                throw new IllegalArgumentException(
                    String.format("tenant_id required for TENANT job %s", jobKey));
            }
        }
    }
}
