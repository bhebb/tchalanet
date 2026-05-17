package com.tchalanet.server.core.sales.internal.domain.service;

import com.tchalanet.server.core.sales.api.config.TicketVisibilityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class TicketVisibilityPolicy {

    private final TicketVisibilityProperties props;
    private final Clock clock;

    public boolean isPubliclyVisible(Instant placedAt) {
        if (placedAt == null) return false;
        var visibilityDays = Math.max(1, props.publicVisibilityDays());
        var expiresAt = placedAt.plus(Duration.ofDays(visibilityDays));
        return !expiresAt.isBefore(Instant.now(clock));
    }
}
