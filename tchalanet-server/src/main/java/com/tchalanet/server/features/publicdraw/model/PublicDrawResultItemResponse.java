package com.tchalanet.server.features.publicdraw.model;

import java.time.Instant;
import java.time.LocalDate;

public record PublicDrawResultItemResponse(
    String drawId,
    String slotKey,
    String provider,
    String timezone,
    String drawTime,
    LocalDate drawDate,
    Instant occurredAt,
    String lot1,
    String lot2,
    String lot3,
    String lot4,
    String status,
    String quality,
    String source
) {}
