package com.tchalanet.server.core.draw.infra.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class DrawSchedulerWindows {

    private final DrawProperties props;

    public boolean isInFetchResultsWindow(LocalTime now) {
        return matches(props.getScheduler().getWindows().getFetchResults(), now);
    }

    public boolean isInSettleDrawsWindow(LocalTime now) {
        return matches(props.getScheduler().getWindows().getSettleDraws(), now);
    }

    public boolean isInCloseDrawsWindow(LocalTime now) {
        return matches(props.getScheduler().getWindows().getCloseDraws(), now);
    }

    public boolean isInOpenDrawsWindow(LocalTime now) {
        return matches(props.getScheduler().getWindows().getOpenDraws(), now);
    }

    private TimeRange toRange(String token) {
        var parts = token.split("-");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid draw scheduler time range: " + token);
        }
        return new TimeRange(LocalTime.parse(parts[0]), LocalTime.parse(parts[1]));
    }

    private record TimeRange(LocalTime from, LocalTime to) {
        boolean contains(LocalTime t) {
            if (from.equals(to)) return true;
            if (!from.isAfter(to)) {
                return !t.isBefore(from) && !t.isAfter(to);
            }
            return !t.isBefore(from) || !t.isAfter(to);
        }
    }

    private boolean matches(String value, LocalTime now) {
        var windows = props.getScheduler().getWindows();

        if (!props.getScheduler().isActive() || !windows.isEnabled()) {
            return true;
        }

        if (value == null || value.isBlank()) {
            return true;
        }

        return Arrays.stream(value.split(","))
            .map(String::trim)
            .map(this::toRange)
            .anyMatch(r -> r.contains(now));
    }
}
