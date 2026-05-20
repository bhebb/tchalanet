package com.tchalanet.server.core.drawresult.internal.application.view;

import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.LocalDate;

public record PublicDrawResultView(
    LocalDate resultDate,
    Instant occurredAt,
    String status,
    String quality,
    JsonNode haiti,
    JsonNode source
) {
}
