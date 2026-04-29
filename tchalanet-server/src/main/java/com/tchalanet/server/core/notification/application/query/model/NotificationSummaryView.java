package com.tchalanet.server.core.notification.application.query.model;

public record NotificationSummaryView(
    long unreadCount,
    long criticalCount,
    long actionRequiredCount,
    boolean hasActionRequired) {}
