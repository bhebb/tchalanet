package com.tchalanet.server.platform.notification.api.model.request;

import java.time.Instant;
import java.util.Collection;

public record ExpireNotificationsByDedupeKeysRequest(Collection<String> dedupeKeys, Instant now) {}
