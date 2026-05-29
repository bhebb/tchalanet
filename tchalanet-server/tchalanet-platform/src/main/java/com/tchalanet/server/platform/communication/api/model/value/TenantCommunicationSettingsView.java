package com.tchalanet.server.platform.communication.api.model.value;

public record TenantCommunicationSettingsView(
    boolean emailEnabled,
    String criticalAlertEmail,
    String opsAlertEmail,
    String defaultLocale
) {}
