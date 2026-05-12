package com.tchalanet.server.features.pagemodel.dashboard;

import com.tchalanet.server.core.pagemodel.domain.model.PageModelDoc;
import com.tchalanet.server.core.notification.application.query.model.NotificationSummaryView;
import com.tchalanet.server.features.pagemodel.shared.PageDynamicPayload;
import java.util.List;

public record DashboardPageModelResponse(
    String currentLang,
    List<String> langs,
    PageModelDoc pageModel,
    PageDynamicPayload dynamic,
    NotificationSummaryView notifications
) {}
