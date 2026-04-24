package com.tchalanet.server.features.pagemodel;

import java.util.List;
import java.util.Map;

public record PageDynamicPayload(
    Map<String, Object> widgets,
    List<WidgetDynamicError> errors
) {}
