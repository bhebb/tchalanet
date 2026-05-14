package com.tchalanet.server.app.job.registry;

import com.tchalanet.server.common.job.key.JobKey;

public final class AppBatchJobKeys {

    private AppBatchJobKeys() {}

    public static final JobKey BATCH_GLOBAL_ENABLED = JobKey.of("batch:global:enabled");

    public static final JobKey DRAW_GENERATE = JobKey.of("draw:lifecycle:generate");
    public static final JobKey DRAW_OPEN = JobKey.of("draw:lifecycle:open");
    public static final JobKey DRAW_CLOSE = JobKey.of("draw:lifecycle:close");
    public static final JobKey DRAW_SETTLE = JobKey.of("draw:lifecycle:settle");

    public static final JobKey RESULTS_EXTERNAL_FETCH = JobKey.of("results:external:fetch");
    public static final JobKey RESULTS_EXTERNAL_APPLY = JobKey.of("results:external:apply");

    public static final JobKey CATALOG_SEARCH_REINDEX = JobKey.of("catalog:search:reindex");
}
