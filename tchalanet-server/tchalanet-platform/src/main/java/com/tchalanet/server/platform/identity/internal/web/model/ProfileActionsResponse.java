package com.tchalanet.server.platform.identity.internal.web.model;

public record ProfileActionsResponse(
    boolean canEditDisplayName,
    boolean canEditLocale,
    boolean canEditTimezone,
    boolean canChangePassword) {}
