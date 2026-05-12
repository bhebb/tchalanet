package com.tchalanet.server.platform.notification.api.model;

public record NotificationSummaryView(
    long unreadCount,
    long criticalCount,
    long actionRequiredCount,
    boolean hasActionRequired) {}
