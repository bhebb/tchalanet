package com.tchalanet.server.core.draw.infra.web.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateDrawChannelRequest(
    @NotNull UUID tenantId, @NotBlank String code, @NotBlank String name, boolean active) {}
