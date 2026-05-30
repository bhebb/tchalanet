package com.tchalanet.server.core.draw.internal.infra.web.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelDrawRequest(
    @NotBlank @Size(max = 96) String reasonCode,
    @Size(max = 255) String reasonLabel,
    boolean force
) {}
