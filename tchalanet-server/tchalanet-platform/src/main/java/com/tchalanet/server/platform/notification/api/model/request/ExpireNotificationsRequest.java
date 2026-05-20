package com.tchalanet.server.platform.notification.api.model.request;

import java.time.Instant;

public record ExpireNotificationsRequest(Instant now) {}
