package com.tchalanet.server.common.batch.key;

import lombok.experimental.UtilityClass;

/**
 * Predefined JobKey constants used across the codebase.
 */
@UtilityClass
public class BatchJobKeys {

    public static final JobKey BATCH_GLOBAL_ENABLED = JobKey.of("batch:global:enabled");
    public static final JobKey DRAW_GENERATE = JobKey.of("draw:lifecycle:generate");
    public static final JobKey DRAW_OPEN = JobKey.of("draw:lifecycle:open");
    public static final JobKey DRAW_CLOSE = JobKey.of("draw:lifecycle:close");
    public static final JobKey DRAW_PROCESSING = JobKey.of("draw:processing");
    public static final JobKey SALES_SESSION_AUTO = JobKey.of("salessession:open-close:auto");
    public static final JobKey DRAW_SETTLE = JobKey.of("draw:lifecycle:settle");
    public static final JobKey DRAW_WATCHDOG_PROVISIONAL = JobKey.of("draw:watchdog:provisional");

    public static final JobKey RESULTS_EXTERNAL_APPLY = JobKey.of("results:external:apply");
    public static final JobKey RESULTS_EXTERNAL_FETCH = JobKey.of("results:external:fetch");
    public static final JobKey RESULTS_EXTERNAL_REFRESH = JobKey.of("results:external:refresh");
    public static final JobKey RESULTS_EXTERNAL_MANUAL = JobKey.of("results:external:manual");
    public static final JobKey RESULTS_EXTERNAL_OVERRIDE = JobKey.of("results:external:override");
}
