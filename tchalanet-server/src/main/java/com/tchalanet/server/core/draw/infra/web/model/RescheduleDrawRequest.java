package com.tchalanet.server.core.draw.infra.web.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record RescheduleDrawRequest(
    @NotNull Instant scheduledAt,
    @NotNull Instant cutoffAt,
    @Size(max = 255) String reason,
    boolean force
) {}
