package com.tchalanet.server.core.sellerterminal.api.model;

public record SellerTerminalSettingsView(
    SellerTerminalReceiptSettingsView receipt,
    SellerTerminalNotificationSettingsView notifications) {
  public static SellerTerminalSettingsView defaults() {
    return new SellerTerminalSettingsView(
        SellerTerminalReceiptSettingsView.defaults(),
        SellerTerminalNotificationSettingsView.defaults());
  }
}
