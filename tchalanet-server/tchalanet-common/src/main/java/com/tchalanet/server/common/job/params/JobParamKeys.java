package com.tchalanet.server.common.job.params;

/**
 * Technical parameter keys understood by the generic job/batch runtime.
 *
 * <p>Domain-specific parameters such as slot_key, provider, days_ahead,
 * channel_code, max_draws, etc. belong to the owning core/catalog module.
 */
public final class JobParamKeys {

    private JobParamKeys() {}

    /** Tenant execution context. Required for tenant-scoped jobs. */
    public static final String TENANT_ID = "tenant_id";

    /** Correlation/request id for logs and audit. Generated if missing. */
    public static final String REQUEST_ID = "request_id";

    /** Technical actor that triggered the job: ops, scheduler, batch, system, etc. */
    public static final String ACTOR = "actor";

    /** Non-destructive execution mode. */
    public static final String DRY_RUN = "dry_run";

    /** Explicit operator override, must be authorized and audited by caller. */
    public static final String FORCE = "force";

    /** Generic execution cap. Domain-specific caps should use domain keys. */
    public static final String MAX_ITEMS = "max_items";

    /** Spring Batch identifying timestamp parameter. */
    public static final String TS = "ts";
}
