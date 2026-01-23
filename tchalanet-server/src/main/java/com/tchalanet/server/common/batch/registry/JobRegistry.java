package com.tchalanet.server.common.batch.registry;

import com.tchalanet.server.common.batch.key.BatchJobKeys;
import com.tchalanet.server.common.batch.key.JobKey;
import com.tchalanet.server.common.batch.params.BatchParamKeys;
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
@Component
public class JobRegistry {

    private final Map<JobKey, RegisteredJob> registry;

    public JobRegistry() {
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
            Set.of(BatchParamKeys.TENANT_ID),
            Set.of(
                BatchParamKeys.REQUEST_ID,
                BatchParamKeys.ACTOR,
                BatchParamKeys.TENANT_ZONE_ID,     // check-only
                BatchParamKeys.TENANT_CURRENCY,    // check-only
                BatchParamKeys.DAYS_AHEAD,
                BatchParamKeys.DRY_RUN
            ),
            "generateDrawsJob"
        ));

        register(map, new RegisteredJob(
            BatchJobKeys.DRAW_OPEN,
            "Open draws in time window",
            RegisteredJob.JobScope.TENANT,
            Set.of(BatchParamKeys.TENANT_ID),
            Set.of(
                BatchParamKeys.REQUEST_ID,
                BatchParamKeys.ACTOR,
                BatchParamKeys.TENANT_ZONE_ID,     // check-only
                BatchParamKeys.TENANT_CURRENCY,    // check-only
                BatchParamKeys.FROM,
                BatchParamKeys.TO
            ),
            "openDrawsJob"
        ));

        register(map, new RegisteredJob(
            BatchJobKeys.DRAW_CLOSE,
            "Close draws in time window",
            RegisteredJob.JobScope.TENANT,
            Set.of(BatchParamKeys.TENANT_ID),
            Set.of(
                BatchParamKeys.REQUEST_ID,
                BatchParamKeys.ACTOR,
                BatchParamKeys.TENANT_ZONE_ID,     // check-only
                BatchParamKeys.TENANT_CURRENCY,    // check-only
                BatchParamKeys.FROM,
                BatchParamKeys.TO
            ),
            "closeDrawsJob"
        ));

        register(map, new RegisteredJob(
            BatchJobKeys.DRAW_SETTLE,
            "Settle draws with results",
            RegisteredJob.JobScope.TENANT,
            Set.of(BatchParamKeys.TENANT_ID),
            Set.of(
                BatchParamKeys.REQUEST_ID,
                BatchParamKeys.ACTOR,
                BatchParamKeys.TENANT_ZONE_ID,     // check-only
                BatchParamKeys.TENANT_CURRENCY,    // check-only
                BatchParamKeys.DATE,
                BatchParamKeys.MAX_ITEMS
            ),
            "settleDrawsJob"
        ));

        // -----------------------------
        // Results pipeline (TENANT)
        // -----------------------------
        register(map, new RegisteredJob(
            BatchJobKeys.RESULTS_EXTERNAL_REFRESH,
            "Refresh external results from providers",
            RegisteredJob.JobScope.TENANT,
            Set.of(BatchParamKeys.TENANT_ID),
            Set.of(
                BatchParamKeys.REQUEST_ID,
                BatchParamKeys.ACTOR,
                BatchParamKeys.TENANT_ZONE_ID,     // check-only
                BatchParamKeys.TENANT_CURRENCY,    // check-only
                BatchParamKeys.SLOT_KEY,
                BatchParamKeys.OCCURRED_AT
            ),
            "refreshExternalResultsJob"
        ));

        register(map, new RegisteredJob(
            BatchJobKeys.RESULTS_EXTERNAL_FETCH,
            "Fetch results for slots",
            RegisteredJob.JobScope.TENANT,
            Set.of(BatchParamKeys.TENANT_ID),
            Set.of(
                BatchParamKeys.REQUEST_ID,
                BatchParamKeys.ACTOR,
                BatchParamKeys.TENANT_ZONE_ID,     // check-only
                BatchParamKeys.TENANT_CURRENCY,    // check-only
                BatchParamKeys.FROM,
                BatchParamKeys.TO
            ),
            "fetchResultsJob"
        ));

        register(map, new RegisteredJob(
            BatchJobKeys.RESULTS_EXTERNAL_APPLY,
            "Apply fetched results to draws",
            RegisteredJob.JobScope.TENANT,
            Set.of(BatchParamKeys.TENANT_ID),
            Set.of(
                BatchParamKeys.REQUEST_ID,
                BatchParamKeys.ACTOR,
                BatchParamKeys.TENANT_ZONE_ID,     // check-only
                BatchParamKeys.TENANT_CURRENCY,    // check-only
                BatchParamKeys.DATE
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
                BatchParamKeys.REQUEST_ID,
                BatchParamKeys.ACTOR,
                BatchParamKeys.FULL_REBUILD,
                BatchParamKeys.MAX_ITEMS
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
