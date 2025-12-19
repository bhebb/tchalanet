package com.tchalanet.server.core.draw.infra.web.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record CreateDrawRequest(@NotNull UUID tenantId, @NotBlank String channelCode, @NotNull LocalDate scheduledDate) {}
