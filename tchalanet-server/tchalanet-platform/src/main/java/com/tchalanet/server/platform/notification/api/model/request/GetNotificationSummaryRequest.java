package com.tchalanet.server.platform.notification.api.model.request;

import com.tchalanet.server.common.types.id.UserId;

public record GetNotificationSummaryRequest(UserId userId, String roleCode) {}
