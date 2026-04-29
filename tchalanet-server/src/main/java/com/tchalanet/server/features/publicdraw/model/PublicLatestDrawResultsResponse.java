package com.tchalanet.server.features.publicdraw.model;

import java.time.Instant;
import java.util.List;

public record PublicLatestDrawResultsResponse(
    String slotKey,
    String provider,
    String timezone,
    String drawTime,
    Instant nextScheduledAt,
    List<PublicDrawResultItemResponse> results) {}
