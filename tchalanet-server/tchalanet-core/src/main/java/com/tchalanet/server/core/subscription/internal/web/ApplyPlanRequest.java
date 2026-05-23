package com.tchalanet.server.core.subscription.internal.web;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public record ApplyPlanRequest(
    @NotBlank String planCode,
    Instant effectiveAt
) {}
