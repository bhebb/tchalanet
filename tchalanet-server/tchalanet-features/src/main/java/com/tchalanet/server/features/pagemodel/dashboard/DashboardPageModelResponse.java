package com.tchalanet.server.features.pagemodel.dashboard;

import com.tchalanet.server.core.pagemodel.internal.domain.model.PageModelDoc;
import com.tchalanet.server.platform.notification.api.model.view.NotificationSummaryView;
import com.tchalanet.server.features.pagemodel.shared.PageDynamicPayload;
import java.util.List;

public record DashboardPageModelResponse(
    String currentLang,
    List<String> langs,
    PageModelDoc pageModel,
    PageDynamicPayload dynamic,
    NotificationSummaryView notifications
) {}
