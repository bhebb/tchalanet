package com.tchalanet.server.features.privatedashboard;

import com.tchalanet.server.core.pagemodel.domain.model.PageModelDoc;
import com.tchalanet.server.features.privatedashboard.block.PrivateDashboardDynamicPayload;
import java.util.List;
import java.util.Map;

public record PrivateDashboardResponse(
    String currentLang,
    List<String> langs,
    PageModelDoc pageModel,
    PrivateDashboardDynamicPayload dynamic,
    Map<String, Object> i18n) {}
