package com.tchalanet.server.common.batch.key;

/**
 * Predefined JobKey constants used across the codebase.
 */
public final class BatchJobKeys {


    private BatchJobKeys() {
    }

    public static final JobKey DRAW_GENERATE = JobKey.of("draw:lifecycle:generate");
    public static final JobKey DRAW_OPEN = JobKey.of("draw:lifecycle:open");
    public static final JobKey DRAW_CLOSE = JobKey.of("draw:lifecycle:close");
    public static final JobKey DRAW_SETTLE = JobKey.of("draw:lifecycle:settle");

    public static final JobKey RESULTS_EXTERNAL_APPLY = JobKey.of("results:external:apply");
    public static final JobKey RESULTS_EXTERNAL_FETCH = JobKey.of("results:external:fetch");
    public static final JobKey RESULTS_EXTERNAL_REFRESH = JobKey.of("results:external:refresh");
}
