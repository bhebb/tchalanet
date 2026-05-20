package com.tchalanet.server.core.draw.internal.infra.web.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record UpdateDrawChannelRequest(
    @NotNull UUID tenantId,
    @NotNull UUID id,
    @NotBlank String code,
    @NotBlank String name,
    boolean active) {}
