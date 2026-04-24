package com.tchalanet.server.features.pagemodel_backup.shared;

import java.util.List;
import java.util.Map;

/**
 * DTOs returned by public page model resolution API.
 */
public record PageModelResponse(
    String currentLang,
    List<String> langs,
    PageModel pageModel,
    PageDynamicPayload dynamic,
    Map<String, String> i18nOverrides
) {}

public record PageDynamicPayload(
    Map<String, Object> widgets,
    List<WidgetDynamicError> errors
) {}

public record WidgetDynamicError(
    String widgetId,
    String provider,
    String code,
    String message
) {}

