package com.tchalanet.server.core.terminal.infra.web.admin.model;

import jakarta.validation.constraints.NotNull;

public record SetOperationalControlRequest(
    @NotNull Boolean blocked,
    String reason
) {}
