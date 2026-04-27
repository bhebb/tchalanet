package com.tchalanet.server.features.pagemodel.shared;

public record WidgetDynamicError(
    String widgetId,
    String provider,
    String code,
    String message
) {}

