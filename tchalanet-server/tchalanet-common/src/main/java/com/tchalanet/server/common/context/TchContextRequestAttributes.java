package com.tchalanet.server.common.context;

import lombok.experimental.UtilityClass;

/**
 * HTTP request attribute keys for the provider-neutral access-context pipeline.
 *
 * <p>Pipeline order:
 * <ol>
 *   <li>IdentityBootstrapFilter sets {@link #BOOTSTRAPPED_ACTOR}.</li>
 *   <li>AccessResolutionFilter reads BOOTSTRAPPED_ACTOR, sets {@link #RESOLVED_ACCESS}
 *       and optionally {@link #TENANT_OVERRIDE}.</li>
 *   <li>TchContextFilter reads RESOLVED_ACCESS and builds the canonical {@link TchRequestContext}.</li>
 * </ol>
 *
 * <p>Application code must not read these attributes directly; use {@link TchRequestContext}.
 */
@UtilityClass
public class TchContextRequestAttributes {
    public static final String BOOTSTRAPPED_ACTOR = "tch.bootstrappedActor";
    public static final String RESOLVED_ACCESS = "tch.resolvedAccess";
    public static final String TENANT_OVERRIDE = "tch.tenantOverride";
}
