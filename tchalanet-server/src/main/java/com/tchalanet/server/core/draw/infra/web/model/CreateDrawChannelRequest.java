package com.tchalanet.server.core.draw.infra.web.model;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;
import java.util.List;

public record CreateDrawChannelRequest(
    @NotNull UUID tenantId, @NotBlank String code, @NotBlank String name, boolean active,
    String gameCode, String timezone, String drawTime, Integer cutoffSec, List<String> daysOfWeek, Integer sortOrder,
    String defaultSource, String label) {
}
