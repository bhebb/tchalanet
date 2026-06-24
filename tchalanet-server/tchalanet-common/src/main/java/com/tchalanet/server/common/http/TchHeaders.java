package com.tchalanet.server.common.http;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TchHeaders {

    public static final String APP_ERROR_VERSION = "X-Error-Version";
    public static final String X_DELETED_VISIBILITY = "X-Deleted-Visibility";

    public static final String X_REQUEST_ID = "X-Request-Id";
    public static final String X_TRACE_ID = "X-Trace-Id";
    public static final String X_SPAN_ID = "X-Span-Id";
    public static final String X_FORWARDED_FOR = "X-Forwarded-For";
    public static final String X_TENANT_ID = "X-Tenant-Id";
    public static final String X_TCH_TENANT_OVERRIDE = "X-Tch-Tenant-Override";
    public static final String X_TCH_OVERRIDE_REASON = "X-Tch-Override-Reason";
    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    /**
     * Admin-to-seller-terminal bridge: TENANT_ADMIN/SUPER_ADMIN sends this header to act as a
     * specific seller terminal on POS cashier endpoints. Value = seller terminal UUID.
     * Injected into TchRequestContext.sellerTerminalId by TchContextFilter.
     */
    public static final String X_TCH_ACT_AS_TERMINAL = "X-Tch-Act-As-Terminal";

    /** Resolver hint selecting which identity resolver to use. Never grants access. */
    public static final String X_TCH_CLIENT_TYPE = "X-Tch-Client-Type";
    public static final String CLIENT_TYPE_POS = "POS";
    public static final String CLIENT_TYPE_WEB = "WEB";

}
