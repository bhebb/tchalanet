package com.tchalanet.server.core.drawresult.api.query.view;

import java.time.Instant;
import java.time.LocalDate;
import tools.jackson.databind.JsonNode;

public record PublicDrawResultView(
    LocalDate resultDate,
    Instant occurredAt,
    String status,
    String quality,
    JsonNode haiti,
    JsonNode source,
    /** UUID opaque — identifiant public du draw_result. */
    String drawResultId) {}
