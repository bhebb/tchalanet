package com.tchalanet.server.catalog.plan.api;

import java.util.Set;

public final class PlanFeatureKeys {


    private PlanFeatureKeys() {}

    public static final String TENANT_PROFILE_BASIC = "tenant.profile.basic";
    public static final String AUTH_LOGIN_BASIC = "auth.login.basic";
    public static final String USER_SELF_PROFILE = "user.self.profile";

    public static final String SALES_TICKET_SELL = "sales.ticket.sell";
    public static final String SALES_PHONE_SELL = "sales.phone.sell";
    public static final String SALES_TICKET_LOOKUP = "sales.ticket.lookup";
    public static final String SALES_TICKET_REPRINT = "sales.ticket.reprint";
    public static final String SALES_TICKET_CANCEL_BASIC = "sales.ticket.cancel.basic";
    public static final String SALES_TICKET_VOID_ADMIN = "sales.ticket.void.admin";

    public static final String DRAW_ACTIVE_LIST = "draw.active.list";
    public static final String DRAWRESULT_PUBLIC_VIEW = "drawresult.public.view";

    public static final String PAYOUT_BASIC = "payout.basic";
    public static final String PAYOUT_SESSION_BASIC = "payout.session.basic";
    public static final String PAYOUT_ADMIN_REVIEW = "payout.admin.review";
    public static final String PAYOUT_APPROVAL_WORKFLOW = "payout.approval.workflow";

    public static final String DOCUMENT_RECEIPT_BASIC = "document.receipt.basic";
    public static final String DOCUMENT_RECEIPT_LOGO = "document.receipt.logo";
    public static final String DOCUMENT_RECEIPT_PDF = "document.receipt.pdf";
    public static final String DOCUMENT_RECEIPT_CUSTOM_TEMPLATE_BASIC = "document.receipt.custom_template.basic";

    public static final String POS_WEB_BASIC = "pos.web.basic";
    public static final String MOBILE_POS_BASIC = "mobile.pos.basic";
    public static final String MOBILE_DEVICE_MANAGEMENT = "mobile.device.management";

    public static final String USER_MANAGEMENT_BASIC = "user.management.basic";
    public static final String USER_MANAGEMENT_STANDARD = "user.management.standard";
    public static final String USER_ROLE_ASSIGNMENT_BASIC = "user.role.assignment.basic";

    public static final String OUTLET_MANAGEMENT_BASIC = "outlet.management.basic";
    public static final String OUTLET_MANAGEMENT_MULTI = "outlet.management.multi";

    public static final String TERMINAL_MANAGEMENT_BASIC = "terminal.management.basic";
    public static final String TERMINAL_LICENSING = "terminal.licensing";
    public static final String TERMINAL_DEVICE_BINDING = "terminal.device.binding";

    public static final String SESSION_CASHIER_BASIC = "session.cashier.basic";
    public static final String SESSION_SUPERVISION = "session.supervision";

    public static final String REPORTING_DAILY_BASIC = "reporting.daily.basic";
    public static final String REPORTING_SALES_SUMMARY = "reporting.sales.summary";
    public static final String REPORTING_PAYOUT_SUMMARY = "reporting.payout.summary";
    public static final String REPORTING_DASHBOARD_STANDARD = "reporting.dashboard.standard";
    public static final String REPORTING_DASHBOARD_PRO = "reporting.dashboard.pro";
    public static final String REPORTING_EXPORT_CSV = "reporting.export.csv";
    public static final String REPORTING_EXPORT_EXCEL = "reporting.export.excel";

    public static final String NOTIFICATION_IN_APP = "notification.in_app";
    public static final String NOTIFICATION_EMAIL = "notification.email";

    public static final String TENANT_THEME_LOGO = "tenant.theme.logo";
    public static final String TENANT_THEME_BASIC_BRANDING = "tenant.theme.basic_branding";
    public static final String THEME_PRESET_SELECTION = "theme.preset_selection";
    public static final String THEME_CUSTOM_TOKENS = "theme.custom_tokens";
    public static final String THEME_CUSTOM_FONT = "theme.custom_font";

    public static final String TENANTGAME_MANAGEMENT = "tenantgame.management";
    public static final String TENANTGAME_SETTINGS = "tenantgame.settings";
    public static final String TENANTGAME_AVAILABILITY = "tenantgame.availability";

    public static final String SECURITY_ROLE_BASIC = "security.role.basic";
    public static final String AUDIT_BASIC = "audit.basic";
    public static final String AUDIT_VIEWER = "audit.viewer";

    public static final String LIMITPOLICY_BASIC = "limitpolicy.basic";
    public static final String LIMITPOLICY_ADVANCED = "limitpolicy.advanced";

    public static final String OFFLINE_SALES_BASIC = "offline.sales.basic";
    public static final String OFFLINE_SYNC_REVIEW = "offline.sync.review";
    public static final String OFFLINE_GRANT_BASIC = "offline.grant.basic";

    public static final String PROMOTION_CAMPAIGN_ADMIN = "promotion.campaigns.config";
    public static final String PROMOTION_RULES_BASIC = "promotion.rules.basic";
    public static final String PROMOTION_FREE_GAME = "promotion.free_game";
    public static final String PROMOTION_PRIZE_MULTIPLIER = "promotion.prize_multiplier";

    public static final String DEMO_FULL_ACCESS = "demo.full_access";
    public static final String DEMO_SEED_DATA = "demo.seed_data";
    public static final String DEMO_EXTERNAL_DELIVERY_MOCK = "demo.external_delivery.mock";
    public static final String DEMO_EXPIRES = "demo.expires";

    public static final Set<String> ALL = Set.of(
        TENANT_PROFILE_BASIC,
        AUTH_LOGIN_BASIC,
        USER_SELF_PROFILE,
        SALES_TICKET_SELL,
        SALES_PHONE_SELL,
        SALES_TICKET_LOOKUP,
        SALES_TICKET_REPRINT,
        SALES_TICKET_CANCEL_BASIC,
        SALES_TICKET_VOID_ADMIN,
        DRAW_ACTIVE_LIST,
        DRAWRESULT_PUBLIC_VIEW,
        PAYOUT_BASIC,
        PAYOUT_SESSION_BASIC,
        PAYOUT_ADMIN_REVIEW,
        PAYOUT_APPROVAL_WORKFLOW,
        DOCUMENT_RECEIPT_BASIC,
        DOCUMENT_RECEIPT_LOGO,
        DOCUMENT_RECEIPT_PDF,
        DOCUMENT_RECEIPT_CUSTOM_TEMPLATE_BASIC,
        POS_WEB_BASIC,
        MOBILE_POS_BASIC,
        MOBILE_DEVICE_MANAGEMENT,
        USER_MANAGEMENT_BASIC,
        USER_MANAGEMENT_STANDARD,
        USER_ROLE_ASSIGNMENT_BASIC,
        OUTLET_MANAGEMENT_BASIC,
        OUTLET_MANAGEMENT_MULTI,
        TERMINAL_MANAGEMENT_BASIC,
        TERMINAL_LICENSING,
        TERMINAL_DEVICE_BINDING,
        SESSION_CASHIER_BASIC,
        SESSION_SUPERVISION,
        REPORTING_DAILY_BASIC,
        REPORTING_SALES_SUMMARY,
        REPORTING_PAYOUT_SUMMARY,
        REPORTING_DASHBOARD_STANDARD,
        REPORTING_DASHBOARD_PRO,
        REPORTING_EXPORT_CSV,
        REPORTING_EXPORT_EXCEL,
        NOTIFICATION_IN_APP,
        NOTIFICATION_EMAIL,
        TENANT_THEME_LOGO,
        TENANT_THEME_BASIC_BRANDING,
        THEME_PRESET_SELECTION,
        THEME_CUSTOM_TOKENS,
        THEME_CUSTOM_FONT,
        TENANTGAME_MANAGEMENT,
        TENANTGAME_SETTINGS,
        TENANTGAME_AVAILABILITY,
        SECURITY_ROLE_BASIC,
        AUDIT_BASIC,
        AUDIT_VIEWER,
        LIMITPOLICY_BASIC,
        LIMITPOLICY_ADVANCED,
        OFFLINE_SALES_BASIC,
        OFFLINE_SYNC_REVIEW,
        OFFLINE_GRANT_BASIC,
        PROMOTION_RULES_BASIC,
        PROMOTION_FREE_GAME,
        PROMOTION_PRIZE_MULTIPLIER,
        DEMO_FULL_ACCESS,
        DEMO_SEED_DATA,
        DEMO_EXTERNAL_DELIVERY_MOCK,
        DEMO_EXPIRES
    );
}
