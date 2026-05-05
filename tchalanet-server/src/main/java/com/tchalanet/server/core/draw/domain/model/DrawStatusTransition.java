package com.tchalanet.server.core.draw.domain.model;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class DrawStatusTransition {

    private static final Map<DrawStatus, Set<DrawStatus>> ALLOWED =
        Map.of(
            DrawStatus.SCHEDULED, Set.of(DrawStatus.OPEN, DrawStatus.CANCELED),
            DrawStatus.OPEN, Set.of(DrawStatus.CLOSED, DrawStatus.CANCELED),
            DrawStatus.CLOSED, Set.of(DrawStatus.RESULTED, DrawStatus.CANCELED),
            DrawStatus.RESULTED, Set.of(DrawStatus.SETTLED),
            DrawStatus.SETTLED, Set.of(DrawStatus.ARCHIVED),
            DrawStatus.CANCELED, Set.of(DrawStatus.ARCHIVED),
            DrawStatus.ARCHIVED, Set.of());

    private DrawStatusTransition() {}

    public static void check(DrawStatus from, DrawStatus to) {
        Objects.requireNonNull(from, "from status is required");
        Objects.requireNonNull(to, "to status is required");

        if (!ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw new IllegalStateException("Invalid draw status transition: " + from + " -> " + to);
        }
    }

    public static boolean canTransition(DrawStatus from, DrawStatus to) {
        Objects.requireNonNull(from, "from status is required");
        Objects.requireNonNull(to, "to status is required");

        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }
}
