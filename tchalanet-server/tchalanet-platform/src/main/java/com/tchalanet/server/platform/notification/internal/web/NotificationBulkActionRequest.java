package com.tchalanet.server.platform.notification.internal.web;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record NotificationBulkActionRequest(@NotEmpty List<UUID> ids) {}
