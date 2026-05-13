package com.tchalanet.server.common.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TchHeaders {

    public static final String API_VERSION = "X-Api-Version";
    public static final String APP_VERSION = "X-App-Version";
    public static final String APP_ERROR_VERSION = "X-Error-Version";
    public static final String X_DELETED_VISIBILITY = "X-Deleted-Visibility";

    public static final String X_REQUEST_ID = "X-Request-Id";
    public static final String X_FORWARDED_FOR = "X-Forwarded-For";
    public static final String X_TENANT_ID = "X-Tenant-Id";
    public static final String X_TCH_TENANT_OVERRIDE = "X-Tch-Tenant-Override";
    public static final String X_TCH_OVERRIDE_REASON = "X-Tch-Override-Reason";
    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    public static final String X_TERMINAL_ID = "X-Terminal-Id";
    public static final String X_TCH_TERMINAL_ID = "X-Tch-Terminal-Id";
    public static final String X_TCH_OUTLET_ID = "X-Tch-Outlet-Id";
    public static final String X_TCH_SALES_SESSION_ID = "X-Tch-Sales-Session-Id";
    public static final String X_TCH_OPERATIONAL_SOURCE = "X-Tch-Operational-Source";
    public static final String X_DEVICE_ID = "X-Device-Id";
    public static final String X_TERMINAL_BINDING = "X-Terminal-Binding";
    public static final String X_OUTLET_ID = "X-OUTLET-ID";

}
