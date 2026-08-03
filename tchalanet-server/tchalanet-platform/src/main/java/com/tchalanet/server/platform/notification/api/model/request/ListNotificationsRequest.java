package com.tchalanet.server.platform.notification.api.model.request;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchSearchQuery;
import com.tchalanet.server.platform.notification.api.model.NotificationCategory;
import com.tchalanet.server.platform.notification.api.model.NotificationKind;
import com.tchalanet.server.platform.notification.api.model.NotificationSeverity;
import com.tchalanet.server.platform.notification.api.model.NotificationStatus;
import java.util.Optional;

public record ListNotificationsRequest(
    TenantId tenantId,
    UserId userId,
    String roleCode,
    Optional<NotificationStatus> status,
    Optional<NotificationCategory> category,
    Optional<NotificationKind> kind,
    Optional<NotificationSeverity> severity,
    TchSearchQuery search,
    TchPageRequest pageRequest) {}
