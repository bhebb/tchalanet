package com.tchalanet.server.features.cashier.model;

public record CashierPrintableReceipt(
    CashierPrintFormat format,
    String contentType,
    String filename,
    String base64
) {}
