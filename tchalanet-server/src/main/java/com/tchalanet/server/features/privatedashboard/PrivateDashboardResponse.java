package com.tchalanet.server.features.privatedashboard;

import com.tchalanet.server.features.pagemodel.shared.PageModel;
import com.tchalanet.server.features.privatedashboard.block.PrivateDashboardDynamicPayload;
import java.util.List;
import java.util.Map;

public record PrivateDashboardResponse(
    String currentLang,
    List<String> langs,
    PageModel pageModel,
    PrivateDashboardDynamicPayload dynamic,
    Map<String, Object> i18n) {}
