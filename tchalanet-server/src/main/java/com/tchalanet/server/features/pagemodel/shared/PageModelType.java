package com.tchalanet.server.features.pagemodel.shared;

public enum PageModelType {

    // Page publique (utilisée quand user non connecté)
    PUBLIC_HOME(
        "public.home",
        "public",
        "home",
        "public_home"
    ),

    // Dashboards privés selon les rôles
    DASHBOARD_SUPERADMIN(
        "private.dashboard.superadmin",
        "private",
        "dashboard",
        "private_dashboard_superadmin"
    ),

    DASHBOARD_TENANT_ADMIN(
        "private.dashboard.tenant_admin",
        "private",
        "dashboard",
        "private_dashboard_tenant_admin"
    ),

    DASHBOARD_OPERATOR(
        "private.dashboard.operator",
        "private",
        "dashboard",
        "private_dashboard_operator"
    ),

    DASHBOARD_CASHIER(
        "private.dashboard.cashier",
        "private",
        "dashboard",
        "private_dashboard_cashier"
    );

    private final String logicalId;
    private final String scope;
    private final String slug;
    private final String context;

    PageModelType(String logicalId, String scope, String slug, String context) {
        this.logicalId = logicalId;
        this.scope = scope;
        this.slug = slug;
        this.context = context;
    }

    public String logicalId() { return logicalId; }
    public String scope() { return scope; }
    public String slug() { return slug; }
    public String context() { return context; }
}

