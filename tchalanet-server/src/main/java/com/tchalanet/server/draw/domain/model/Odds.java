package com.tchalanet.server.draw.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Odds(
    UUID id,
    UUID tenantId,
    String gameCode,
    BigDecimal multiplier,
    Instant validFrom,
    Instant validTo) {}
