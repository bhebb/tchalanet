package com.tchalanet.server.core.sellerterminal.api.model;

public record SellerTerminalReceiptSettingsView(
    boolean autoPrint,
    int copyCount,
    boolean quickSale,
    String printerMode,
    String paperSize,
    String adapterPreference) {}
