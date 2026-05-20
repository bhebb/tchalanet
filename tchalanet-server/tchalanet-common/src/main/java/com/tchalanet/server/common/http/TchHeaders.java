package com.tchalanet.server.common.http;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TchHeaders {

    public static final String APP_ERROR_VERSION = "X-Error-Version";
    public static final String X_DELETED_VISIBILITY = "X-Deleted-Visibility";

    public static final String X_REQUEST_ID = "X-Request-Id";
    public static final String X_FORWARDED_FOR = "X-Forwarded-For";
    public static final String X_TENANT_ID = "X-Tenant-Id";
    public static final String X_TCH_TENANT_OVERRIDE = "X-Tch-Tenant-Override";
    public static final String X_TCH_OVERRIDE_REASON = "X-Tch-Override-Reason";
    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";

}
