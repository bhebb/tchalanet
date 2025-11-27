package com.tchalanet.server.uslottery.web.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RefreshRequest(
    @NotNull UUID tenantId, @NotNull UUID userId // User performing the refresh
    ) {}
