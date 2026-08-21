package com.tchalanet.server.platform.clientdiagnostics.api.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;

public record ClientDiagnosticPolicyRequest(
    @NotNull Instant expiresAt,
    @Min(1) @Max(500) int maxEvents,
    @NotNull @Size(min = 1, max = 9) Set<ClientDiagnosticCategory> categories,
    @NotBlank @Size(max = 240) String reason) {}
