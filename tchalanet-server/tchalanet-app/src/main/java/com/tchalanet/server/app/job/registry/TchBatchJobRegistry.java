package com.tchalanet.server.app.job.registry;

import com.tchalanet.server.common.job.key.BatchJobKeys;
import com.tchalanet.server.common.job.key.JobKey;
import com.tchalanet.server.common.job.params.JobParamKeys;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Central registry of allowed batch jobs.
 * <p>
 * Single source of truth for the allowlist:
 * - No DB
 * - No Spring scanning
 * - Only jobs registered here can be started via ops endpoints
 * <p>
 * Conventions:
 * - TENANT jobs: requiredParams = { tenant_id }
 * - request_id / actor are OPTIONAL (generated/defaulted if absent)
 * - tenant_code is forbidden (tenant comes from tenant_id only)
 * - tenant_zone_id / tenant_currency are OPTIONAL (safety checks only; no overrides)
 */
@Component("tchBatchJobRegistry")
public class TchBatchJobRegistry {

    private final Map<JobKey, RegisteredJob> registry;

    public TchBatchJobRegistry() {
        this.registry = buildAllowlist();
    }

    public Optional<RegisteredJob> find(JobKey jobKey) {
        return Optional.ofNullable(registry.get(jobKey));
    }

    public Map<JobKey, RegisteredJob> list() {
        return Map.copyOf(registry);
    }

    private static Map<JobKey, RegisteredJob> buildAllowlist() {
        Map<JobKey, RegisteredJob> map = new LinkedHashMap<>();

        // -----------------------------
        // Draw lifecycle (TENANT)
        // -----------------------------
        register(map, new RegisteredJob(
            BatchJobKeys.DRAW_GENERATE,
            "Generate draws for next N days",
            RegisteredJob.JobScope.TENANT,
            Set.of(JobParamKeys.TENANT_ID),
            Set.of(
                JobParamKeys.REQUEST_ID,
                JobParamKeys.ACTOR,
                JobParamKeys.TENANT_ZONE_ID,     // check-only
                JobParamKeys.TENANT_CURRENCY,    // check-only
                JobParamKeys.DAYS_AHEAD,
                JobParamKeys.DRY_RUN
            ),
            "generateDrawsJob"
        ));

        register(map, new RegisteredJob(
            BatchJobKeys.DRAW_OPEN,
            "Open draws in time window",
            RegisteredJob.JobScope.TENANT,
            Set.of(JobParamKeys.TENANT_ID),
            Set.of(
                JobParamKeys.REQUEST_ID,
                JobParamKeys.ACTOR,
                JobParamKeys.TENANT_ZONE_ID,     // check-only
                JobParamKeys.TENANT_CURRENCY,    // check-only
                JobParamKeys.FROM,
                JobParamKeys.TO
            ),
            "openDrawsJob"
        ));

        register(map, new RegisteredJob(
            BatchJobKeys.DRAW_CLOSE,
            "Close draws in time window",
            RegisteredJob.JobScope.TENANT,
            Set.of(JobParamKeys.TENANT_ID),
            Set.of(
                JobParamKeys.REQUEST_ID,
                JobParamKeys.ACTOR,
                JobParamKeys.TENANT_ZONE_ID,     // check-only
                JobParamKeys.TENANT_CURRENCY,    // check-only
                JobParamKeys.FROM,
                JobParamKeys.TO
            ),
            "closeDrawsJob"
        ));

        register(map, new RegisteredJob(
            BatchJobKeys.DRAW_SETTLE,
            "Settle draws with results",
            RegisteredJob.JobScope.TENANT,
            Set.of(JobParamKeys.TENANT_ID),
            Set.of(
                JobParamKeys.REQUEST_ID,
                JobParamKeys.ACTOR,
                JobParamKeys.TENANT_ZONE_ID,     // check-only
                JobParamKeys.TENANT_CURRENCY,    // check-only
                JobParamKeys.DATE,
                JobParamKeys.MAX_ITEMS,
                JobParamKeys.DAYS_BACK,
                JobParamKeys.MAX_DRAWS,
                JobParamKeys.DRY_RUN,
                JobParamKeys.FORCE
            ),
            "settleDrawsJob"
        ));

        // -----------------------------
        // Results pipeline (TENANT)
        // -----------------------------
        register(map, new RegisteredJob(
            BatchJobKeys.RESULTS_EXTERNAL_FETCH,
            "Fetch results for slots",
            RegisteredJob.JobScope.GLOBAL,
            Set.of(),
            Set.of(
                JobParamKeys.REQUEST_ID,
                JobParamKeys.ACTOR,
                JobParamKeys.FROM,
                JobParamKeys.TO,
                JobParamKeys.SLOT_KEY,
                JobParamKeys.DATE,
                JobParamKeys.DAYS_BACK,
                JobParamKeys.MAX_SLOTS,
                JobParamKeys.DRY_RUN,
                JobParamKeys.FORCE
            ),
            "fetchResultsJob"
        ));

        register(map, new RegisteredJob(
            BatchJobKeys.RESULTS_EXTERNAL_APPLY,
            "Apply fetched results to draws",
            RegisteredJob.JobScope.TENANT,
            Set.of(JobParamKeys.TENANT_ID),
            Set.of(
                JobParamKeys.REQUEST_ID,
                JobParamKeys.ACTOR,
                JobParamKeys.TENANT_ZONE_ID,     // check-only
                JobParamKeys.TENANT_CURRENCY,    // check-only
                JobParamKeys.DATE,
                JobParamKeys.SLOT_KEY,
                JobParamKeys.DAYS_BACK,
                JobParamKeys.MAX_SLOTS,
                JobParamKeys.DRY_RUN,
                JobParamKeys.FORCE
            ),
            "applyResultsJob"
        ));

        // -----------------------------
        // Catalog (GLOBAL)
        // -----------------------------
        register(map, new RegisteredJob(
            JobKey.of("catalog:search:reindex"),
            "Reindex catalog for search",
            RegisteredJob.JobScope.GLOBAL,
            Set.of(), // no required params
            Set.of(
                JobParamKeys.REQUEST_ID,
                JobParamKeys.ACTOR,
                JobParamKeys.FULL_REBUILD,
                JobParamKeys.MAX_ITEMS
            ),
            "reindexCatalogJob"
        ));

        return map;
    }

    private static void register(Map<JobKey, RegisteredJob> map, RegisteredJob job) {
        if (map.containsKey(job.jobKey())) {
            throw new IllegalStateException("Duplicate job key: " + job.jobKey());
        }
        map.put(job.jobKey(), job);
    }
}
