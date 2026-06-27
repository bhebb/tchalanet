package com.tchalanet.server.app.job.registry;

import com.tchalanet.server.common.job.key.JobKey;
import com.tchalanet.server.common.job.params.JobParamKeys;
import com.tchalanet.server.common.job.registry.RegisteredJob;
import com.tchalanet.server.common.job.registry.TchJobRegistry;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Central registry of allowed Spring Batch jobs.
 *
 * <p>This is the runtime allowlist:
 * <ul>
 *   <li>No DB</li>
 *   <li>No Spring scanning</li>
 *   <li>Only jobs registered here can be started via ops endpoints</li>
 * </ul>
 *
 * <p>The public {@link RegisteredJob} metadata is exposed through {@link TchJobRegistry}.
 * Spring bean names stay internal to app via {@link SpringTchRegisteredJob}.
 */
@Component
public class SpringTchJobRegistry implements TchJobRegistry {

    private final Map<JobKey, SpringTchRegisteredJob> registry;

    public SpringTchJobRegistry() {
        this.registry = buildAllowlist();
    }

    @Override
    public Optional<RegisteredJob> find(JobKey jobKey) {
        return Optional.ofNullable(registry.get(jobKey))
            .map(SpringTchRegisteredJob::metadata);
    }

    public Optional<SpringTchRegisteredJob> findRuntime(JobKey jobKey) {
        return Optional.ofNullable(registry.get(jobKey));
    }

    @Override
    public Collection<RegisteredJob> list() {
        return registry.values().stream()
            .map(SpringTchRegisteredJob::metadata)
            .toList();
    }

    private static Map<JobKey, SpringTchRegisteredJob> buildAllowlist() {
        Map<JobKey, SpringTchRegisteredJob> map = new LinkedHashMap<>();

        register(map, job(
            AppBatchJobKeys.DRAW_GENERATE,
            "Generate draws for next N days",
            RegisteredJob.JobScope.TENANT,
            Set.of(JobParamKeys.TENANT_ID),
            Set.of(
                JobParamKeys.REQUEST_ID,
                JobParamKeys.ACTOR,
                AppJobParamKeys.FROM,
                AppJobParamKeys.TO,
                AppJobParamKeys.DAYS_AHEAD,
                JobParamKeys.DRY_RUN,
                JobParamKeys.FORCE,
                AppJobParamKeys.REASON
            ),
            "generateDrawsJob"
        ));

        register(map, job(
            AppBatchJobKeys.DRAW_OPEN,
            "Open draws in time window",
            RegisteredJob.JobScope.TENANT,
            Set.of(JobParamKeys.TENANT_ID),
            Set.of(
                JobParamKeys.REQUEST_ID,
                JobParamKeys.ACTOR,
                AppJobParamKeys.DATE,
                JobParamKeys.MAX_ITEMS,
                JobParamKeys.DRY_RUN
            ),
            "openDrawsJob"
        ));

        register(map, job(
            AppBatchJobKeys.DRAW_CLOSE,
            "Close draws in time window",
            RegisteredJob.JobScope.TENANT,
            Set.of(JobParamKeys.TENANT_ID),
            Set.of(
                JobParamKeys.REQUEST_ID,
                JobParamKeys.ACTOR,
                JobParamKeys.MAX_ITEMS,
                JobParamKeys.DRY_RUN
            ),
            "closeDrawsJob"
        ));

        register(map, job(
            AppBatchJobKeys.DRAW_SETTLE,
            "Settle draws with results",
            RegisteredJob.JobScope.TENANT,
            Set.of(JobParamKeys.TENANT_ID),
            Set.of(
                JobParamKeys.REQUEST_ID,
                JobParamKeys.ACTOR,
                AppJobParamKeys.DATE,
                JobParamKeys.MAX_ITEMS,
                AppJobParamKeys.DAYS_BACK,
                AppJobParamKeys.MAX_DRAWS,
                JobParamKeys.DRY_RUN,
                JobParamKeys.FORCE
            ),
            "settleDrawsJob"
        ));

        register(map, job(
            AppBatchJobKeys.RESULTS_EXTERNAL_FETCH,
            "Fetch results for slots",
            RegisteredJob.JobScope.GLOBAL,
            Set.of(),
            Set.of(
                JobParamKeys.REQUEST_ID,
                JobParamKeys.ACTOR,
                AppJobParamKeys.FROM,
                AppJobParamKeys.TO,
                AppJobParamKeys.SLOT_KEY,
                AppJobParamKeys.SLOT_KEYS,
                AppJobParamKeys.DATE,
                AppJobParamKeys.DAYS_BACK,
                AppJobParamKeys.MAX_SLOTS,
                JobParamKeys.DRY_RUN,
                JobParamKeys.FORCE,
                AppJobParamKeys.REASON,
                AppJobParamKeys.INCLUDE_RAW
            ),
            "fetchResultsJob"
        ));

        register(map, job(
            AppBatchJobKeys.RESULTS_EXTERNAL_APPLY,
            "Apply fetched results to draws",
            RegisteredJob.JobScope.TENANT,
            Set.of(JobParamKeys.TENANT_ID),
            Set.of(
                JobParamKeys.REQUEST_ID,
                JobParamKeys.ACTOR,
                AppJobParamKeys.DATE,
                AppJobParamKeys.SLOT_KEY,
                AppJobParamKeys.SLOT_KEYS,
                AppJobParamKeys.DAYS_BACK,
                AppJobParamKeys.MAX_SLOTS,
                JobParamKeys.DRY_RUN,
                JobParamKeys.FORCE,
                AppJobParamKeys.REASON
            ),
            "applyResultsJob"
        ));

        register(map, job(
            AppBatchJobKeys.CATALOG_SEARCH_REINDEX,
            "Reindex catalog for search",
            RegisteredJob.JobScope.GLOBAL,
            Set.of(),
            Set.of(
                JobParamKeys.REQUEST_ID,
                JobParamKeys.ACTOR,
                AppJobParamKeys.FULL_REBUILD,
                JobParamKeys.MAX_ITEMS
            ),
            "reindexCatalogJob"
        ));

        return Map.copyOf(map);
    }

    private static SpringTchRegisteredJob job(
        JobKey jobKey,
        String displayName,
        RegisteredJob.JobScope scope,
        Set<String> requiredParams,
        Set<String> optionalParams,
        String springJobBeanName
    ) {
        return new SpringTchRegisteredJob(
            new RegisteredJob(jobKey, displayName, scope, requiredParams, optionalParams),
            springJobBeanName
        );
    }

    private static void register(
        Map<JobKey, SpringTchRegisteredJob> map,
        SpringTchRegisteredJob job
    ) {
        var key = job.metadata().jobKey();

        if (map.containsKey(key)) {
            throw new IllegalStateException("Duplicate job key: " + key);
        }

        map.put(key, job);
    }
}
