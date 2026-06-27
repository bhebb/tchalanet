package com.tchalanet.server.app.job.registry;

/**
 * App/runtime job parameters that are not part of the generic common job runtime.
 *
 * <p>Generic runtime params stay in common.job.params.JobParamKeys.
 */
public final class AppJobParamKeys {

    private AppJobParamKeys() {}

    public static final String FROM = "from";
    public static final String TO = "to";
    public static final String DATE = "date";

    public static final String DAYS_AHEAD = "days_ahead";
    public static final String DAYS_BACK = "days_back";

    public static final String SLOT_KEY = "slot_key";
    public static final String SLOT_KEYS = "slot_keys";

    public static final String MAX_DRAWS = "max_draws";
    public static final String MAX_SLOTS = "max_slots";
    public static final String REASON = "reason";
    public static final String INCLUDE_RAW = "include_raw";

    public static final String FULL_REBUILD = "full_rebuild";
}
