package com.tchalanet.server.core.outlet.internal.infra.web.admin.model;

import jakarta.validation.constraints.NotNull;

public record SetOperationalControlRequest(
    @NotNull Boolean blocked,
    String reason
) {}
