package com.tchalanet.server.features.pos.profile.model;

import java.time.Instant;

public record PosProfileTerminalInfo(
    String id,
    String code,
    String displayName,
    String status,
    boolean ready,
    boolean trusted,
    boolean mustChangePin,
    Instant lastSeenAt,
    String source) {}
