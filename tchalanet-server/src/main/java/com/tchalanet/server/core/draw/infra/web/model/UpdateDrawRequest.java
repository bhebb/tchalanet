package com.tchalanet.server.core.draw.infra.web.model;

import com.tchalanet.server.common.types.id.TenantId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record UpdateDrawRequest(
    @NotNull TenantId tenantId,
    @NotNull TenantId drawId,
    @NotNull LocalDate scheduledDate,
    @NotBlank String code,
    @NotBlank String name) {}
