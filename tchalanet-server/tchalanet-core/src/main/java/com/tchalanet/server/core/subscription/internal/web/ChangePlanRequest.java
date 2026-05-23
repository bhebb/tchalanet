package com.tchalanet.server.core.subscription.internal.web;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public record ChangePlanRequest(
    @NotBlank String newPlanCode,
    Instant effectiveAt
) {}
