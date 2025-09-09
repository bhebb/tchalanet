package com.tchalanet.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ChangePlanRequest(
    @NotNull UUID planId, boolean proration, @NotBlank String idempotencyKey) {}
