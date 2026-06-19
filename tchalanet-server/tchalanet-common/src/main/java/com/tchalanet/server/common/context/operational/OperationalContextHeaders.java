package com.tchalanet.server.common.context.operational;

public final class OperationalContextHeaders {

    private OperationalContextHeaders() {
    }

    public static final String OPERATIONAL_SOURCE = "X-Tch-Operational-Source";
    public static final String SUPER_ADMIN_TENANT_OVERRIDE = "X-Tch-Tenant-Override";
    public static final String SUPER_ADMIN_OVERRIDE_REASON = "X-Tch-Override-Reason";
}
