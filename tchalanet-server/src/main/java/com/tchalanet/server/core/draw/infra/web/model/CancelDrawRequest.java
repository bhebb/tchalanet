package com.tchalanet.server.core.draw.infra.web.model;

import jakarta.validation.constraints.Size;

public record CancelDrawRequest(
    @Size(max = 255) String reason,
    boolean force
) {}
