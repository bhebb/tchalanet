package com.tchalanet.server.core.session.api.query;

/**
 * Identity context for the cashier dashboard — who is logged in and from which POS.
 * Outlet and terminal are null when no session is currently open.
 */
public record CashierIdentityView(
    String cashierDisplayName,
    String outletName,
    String terminalLabel,
    String tenantCode
) {}
