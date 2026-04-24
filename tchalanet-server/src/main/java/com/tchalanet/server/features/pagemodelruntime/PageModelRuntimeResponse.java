package com.tchalanet.server.features.pagemodelruntime;

import com.tchalanet.server.core.pagemodel.domain.model.PageModelDoc;
import com.tchalanet.server.features.pagemodel.PageDynamicPayload;
import java.util.List;

public record PageModelRuntimeResponse(
    String currentLang,
    List<String> langs,
    PageModelDoc pageModel,
    PageDynamicPayload dynamic
) {}

