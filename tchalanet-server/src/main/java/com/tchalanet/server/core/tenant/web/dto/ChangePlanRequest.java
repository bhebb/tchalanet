package com.tchalanet.server.core.tenant.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ChangePlanRequest(
    @NotNull UUID planId, boolean proration, @NotBlank String idempotencyKey) {}
