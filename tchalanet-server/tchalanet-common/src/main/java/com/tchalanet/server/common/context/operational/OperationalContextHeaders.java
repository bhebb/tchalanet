package com.tchalanet.server.common.context.operational;

public final class OperationalContextHeaders {

    private OperationalContextHeaders() {}

    public static final String TERMINAL_ID = "X-Tch-Terminal-Id";
    public static final String OUTLET_ID = "X-Tch-Outlet-Id";
    public static final String SALES_SESSION_ID = "X-Tch-Sales-Session-Id";
    public static final String OPERATIONAL_SOURCE = "X-Tch-Operational-Source";
    public static final String SUPER_ADMIN_TENANT_OVERRIDE = "X-Tch-Tenant-Override";
    public static final String SUPER_ADMIN_OVERRIDE_REASON = "X-Tch-Override-Reason";
}
