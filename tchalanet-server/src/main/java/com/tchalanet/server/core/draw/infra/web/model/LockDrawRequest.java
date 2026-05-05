package com.tchalanet.server.core.draw.infra.web.model;

import jakarta.validation.constraints.Size;

public record LockDrawRequest(
    @Size(max = 255) String reason
) {}
