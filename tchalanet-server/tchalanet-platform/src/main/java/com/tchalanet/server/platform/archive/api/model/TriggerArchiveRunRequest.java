package com.tchalanet.server.platform.archive.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record TriggerArchiveRunRequest(
    @NotBlank String strategy,
    @NotNull LocalDate periodStart,
    @NotNull LocalDate periodEnd,
    @NotBlank @Size(min = 10, max = 500) String reason
) {}
