package com.tchalanet.server.core.sellerterminal.api.model;

public record SellerTerminalSettingsView(
    SellerTerminalReceiptSettingsView receipt,
    SellerTerminalNotificationSettingsView notifications) {}
