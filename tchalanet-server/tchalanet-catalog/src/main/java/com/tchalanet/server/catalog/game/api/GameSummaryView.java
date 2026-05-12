package com.tchalanet.server.catalog.game.api;

import java.time.Instant;

public record GameSummaryView(
    String id,
    String code,
    String name,
    boolean active,
    int sortOrder,
    Instant updatedAt
) {}
