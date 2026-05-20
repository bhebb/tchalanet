package com.tchalanet.server.features.pagemodel.publicpage;

import com.tchalanet.server.core.pagemodel.internal.domain.model.PageModelDoc;
import com.tchalanet.server.features.pagemodel.shared.PageDynamicPayload;
import java.util.List;

public record PublicPageModelResponse(
    String currentLang,
    List<String> langs,
    PageModelDoc pageModel,
    PageDynamicPayload dynamic
) {}

