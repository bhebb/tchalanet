package com.tchalanet.server.core.sellerterminal.api.model;

public record SellerTerminalNotificationSettingsView(boolean enabled, boolean criticalOnly) {
  public static SellerTerminalNotificationSettingsView defaults() {
    return new SellerTerminalNotificationSettingsView(true, false);
  }
}
