package com.tchalanet.server.common.job.params;

public final class JobParamKeys {

    private JobParamKeys() {}

    public static final String TENANT_ID = "tenant_id";

    /** @deprecated forbidden for ops/batch. Tenant is identified by tenant_id only. */
    @Deprecated(forRemoval = false)
    public static final String TENANT_CODE = "tenant_code";

    public static final String TENANT_ZONE_ID = "tenant_zone_id";
    public static final String TENANT_CURRENCY = "tenant_currency";

    public static final String REQUEST_ID = "request_id";
    public static final String ACTOR = "actor";

    /** @deprecated alias for actor (legacy). Prefer actor. */
    @Deprecated(forRemoval = false)
    public static final String TRIGGERED_BY = "triggered_by";

    public static final String OCCURRED_AT = "occurred_at";
    public static final String FROM = "from";
    public static final String TO = "to";
    public static final String DATE = "date";

    public static final String SLOT_KEY = "slot_key";
    // plural form when multiple keys are passed as a param (stringified list)
    public static final String SLOT_KEYS = "slot_keys";

    public static final String DRY_RUN = "dry_run";
    public static final String MAX_ITEMS = "max_items";
    public static final String DAYS_AHEAD = "days_ahead";
    // some jobs use days_back instead of days_ahead
    public static final String DAYS_BACK = "days_back";
    public static final String FULL_REBUILD = "full_rebuild";

    // scheduler / settle specific
    public static final String PROVIDER = "provider";
    public static final String MAX_DRAWS = "max_draws";
    public static final String FORCE = "force";
    public static final String MAX_SLOTS = "max_slots";

    // common reader keys
    public static final String SOURCE = "source";
    public static final String CHANNEL_CODE = "channel_code";

    public static final String TS = "ts";
}
