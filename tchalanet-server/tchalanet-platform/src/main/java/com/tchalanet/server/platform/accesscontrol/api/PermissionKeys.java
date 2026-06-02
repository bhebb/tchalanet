package com.tchalanet.server.platform.accesscontrol.api;

public final class PermissionKeys {
  private PermissionKeys() {}

  // Platform
  public static final String PLATFORM_ACCESS      = "platform.access";
  public static final String PLATFORM_OPS_READ    = "platform.ops.read";
  public static final String PLATFORM_OPS_EXECUTE = "platform.ops.execute";

  // Tenant management
  public static final String TENANT_CREATE        = "tenant.create";
  public static final String TENANT_READ          = "tenant.read";
  public static final String TENANT_UPDATE        = "tenant.update";
  public static final String TENANT_ACTIVATE      = "tenant.activate";
  public static final String TENANT_SUSPEND       = "tenant.suspend";
  public static final String TENANT_ADMIN_CREATE  = "tenant.admin.create";
  public static final String TENANT_OVERRIDE      = "tenant.override";

  // Admin area
  public static final String ADMIN_ACCESS         = "admin.access";
  public static final String DASHBOARD_READ       = "dashboard.read";

  // User management
  public static final String USER_READ            = "user.read";
  public static final String USER_CREATE          = "user.create";
  public static final String USER_UPDATE          = "user.update";
  public static final String USER_DISABLE         = "user.disable";
  public static final String USER_INVITE          = "user.invite";
  public static final String USER_SYNC            = "user.sync";
  public static final String USER_MEMBERSHIP_MANAGE = "user.membership.manage";
  public static final String USER_ROLE_ASSIGN     = "user.role.assign";
  public static final String USER_PERMISSION_MANAGE = "user.permission.manage";

  // Access control
  public static final String ROLE_READ            = "role.read";
  public static final String ROLE_MANAGE          = "role.manage";
  public static final String ROLE_PERMISSION_MANAGE = "role.permission.manage";
  public static final String PERMISSION_READ      = "permission.read";

  // Outlet
  public static final String OUTLET_READ          = "outlet.read";
  public static final String OUTLET_CREATE        = "outlet.create";
  public static final String OUTLET_UPDATE        = "outlet.update";
  public static final String OUTLET_DISABLE       = "outlet.disable";

  // Terminal
  public static final String TERMINAL_READ        = "terminal.read";
  public static final String TERMINAL_CREATE      = "terminal.create";
  public static final String TERMINAL_UPDATE      = "terminal.update";
  public static final String TERMINAL_DISABLE     = "terminal.disable";
  public static final String TERMINAL_BIND        = "terminal.bind";
  public static final String TERMINAL_UNBIND      = "terminal.unbind";

  // Session
  public static final String SESSION_READ         = "session.read";
  public static final String SESSION_OPEN         = "session.open";
  public static final String SESSION_CLOSE        = "session.close";
  public static final String SESSION_FORCE_CLOSE  = "session.force-close";

  // Settings / pricing
  public static final String SETTINGS_READ        = "settings.read";
  public static final String SETTINGS_UPDATE      = "settings.update";
  public static final String GAME_PRICING_READ    = "game-pricing.read";
  public static final String GAME_PRICING_UPDATE  = "game-pricing.update";

  // Limits / promotions
  public static final String LIMIT_READ           = "limit.read";
  public static final String LIMIT_MANAGE         = "limit.manage";
  public static final String PROMOTION_READ       = "promotion.read";
  public static final String PROMOTION_MANAGE     = "promotion.manage";

  // Reports / audit
  public static final String REPORT_READ          = "report.read";
  public static final String AUDIT_READ           = "audit.read";

  // Cashier / operator
  public static final String CASHIER_ACCESS       = "cashier.access";
  public static final String OPERATOR_ACCESS      = "operator.access";

  // Operational context
  public static final String OPERATIONAL_CONTEXT_READ   = "operational-context.read";
  public static final String OPERATIONAL_CONTEXT_SELECT = "operational-context.select";

  // Tickets
  public static final String TICKET_SELL          = "ticket.sell";
  public static final String TICKET_READ          = "ticket.read";
  public static final String TICKET_PRINT         = "ticket.print";
  public static final String TICKET_RESEND        = "ticket.resend";
  public static final String TICKET_VERIFY        = "ticket.verify";
  public static final String TICKET_CANCEL_OWN    = "ticket.cancel-own";

  // Payout
  public static final String PAYOUT_READ          = "payout.read";
  public static final String PAYOUT_REVIEW        = "payout.review";
  public static final String PAYOUT_EXECUTE       = "payout.execute";

  // Offline / sync
  public static final String SYNC_READ            = "sync.read";
  public static final String SYNC_SUBMIT          = "sync.submit";
  public static final String OFFLINE_GRANT_REQUEST = "offline.grant.request";
  public static final String OFFLINE_SYNC_SUBMIT  = "offline.sync.submit";
}
