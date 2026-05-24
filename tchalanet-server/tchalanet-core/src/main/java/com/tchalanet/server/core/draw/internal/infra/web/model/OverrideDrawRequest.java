package com.tchalanet.server.core.draw.internal.infra.web.model;

import com.tchalanet.server.core.draw.api.model.DrawStatus;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record OverrideDrawRequest(
    DrawStatus status, // Nullable
    Instant scheduledAt, // Nullable
    Instant cutoffAt, // Nullable
    @Size(max = 255) String reason,
    boolean force
) {}
