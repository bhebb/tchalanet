package com.tchalanet.server.core.sales.api.model.receipt;

public record TicketReceiptQrView(
    String payload,
    String verificationUrl
) {}
