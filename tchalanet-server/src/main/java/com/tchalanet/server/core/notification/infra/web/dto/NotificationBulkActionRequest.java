package com.tchalanet.server.core.notification.infra.web.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record NotificationBulkActionRequest(@NotEmpty List<UUID> ids) {}
