package com.tchalanet.server.core.drawresult.api.query.view;

import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.LocalDate;

public record PublicDrawResultView(
    LocalDate resultDate,
    Instant occurredAt,
    String status,
    String quality,
    JsonNode haiti,
    JsonNode source,
    /** UUID opaque — identifiant public du draw_result. */
    String drawResultId
) {
}
