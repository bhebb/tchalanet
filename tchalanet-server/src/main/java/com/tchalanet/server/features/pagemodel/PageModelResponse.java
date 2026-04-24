package com.tchalanet.server.features.pagemodel;

import java.util.List;
import java.util.Map;

public record PageModelResponse(
    String currentLang,
    List<String> langs,
    PageModel pageModel,
    PageDynamicPayload dynamic,
    Map<String, String> i18n
) {}
