package com.tchalanet.server.core.draw.internal.infra.web.model;

import jakarta.validation.constraints.Size;

public record ArchiveDrawRequest(
    @Size(max = 255) String reason,
    boolean force
) {}
