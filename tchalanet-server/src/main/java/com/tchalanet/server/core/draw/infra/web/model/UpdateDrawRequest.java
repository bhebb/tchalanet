package com.tchalanet.server.core.draw.infra.web.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateDrawRequest(
    @NotNull UUID tenantId,
    @NotNull UUID drawId,
    @NotNull LocalDate scheduledDate,
    @NotBlank String code,
    @NotBlank String name) {}
