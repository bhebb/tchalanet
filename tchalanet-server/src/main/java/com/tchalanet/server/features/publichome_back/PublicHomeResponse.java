package com.tchalanet.server.features.publichome_back;

import com.tchalanet.server.features.pagemodel_backup.shared.PageModel;
import com.tchalanet.server.features.pagemodel_backup.shared.block.PublicPageDynamicPayload;
import java.util.List;
import java.util.Map;

public record PublicHomeResponse(
    String currentLang,
    List<String> langs,
    PageModel pageModel,
    PublicPageDynamicPayload dynamic,
    Map<String, Object> i18n) {}
