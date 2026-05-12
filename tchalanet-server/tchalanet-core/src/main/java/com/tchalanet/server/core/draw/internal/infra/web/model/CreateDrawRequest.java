package com.tchalanet.server.core.draw.internal.infra.web.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateDrawRequest(
    @NotBlank String channelCode,
    @NotNull LocalDate scheduledDate
) {}
