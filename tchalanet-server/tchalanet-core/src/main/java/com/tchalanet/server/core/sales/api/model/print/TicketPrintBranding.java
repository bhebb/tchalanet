package com.tchalanet.server.core.sales.api.model.print;

public record TicketPrintBranding(
    String tenantDisplayName,
    String tenantReceiptHeader,
    String tenantReceiptFooter,
    String outletReceiptHeader,
    String outletReceiptFooter
) {
}

