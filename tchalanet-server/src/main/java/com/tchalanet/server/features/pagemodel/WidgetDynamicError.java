package com.tchalanet.server.features.pagemodel;

public record WidgetDynamicError(
    String widgetId,
    String provider,
    String code,
    String message
) {}

