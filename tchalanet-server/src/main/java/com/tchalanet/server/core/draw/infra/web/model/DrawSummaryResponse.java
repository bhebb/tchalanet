package com.tchalanet.server.core.draw.infra.web.model;


import tools.jackson.databind.JsonNode;

import java.time.OffsetDateTime;

public record DrawSummaryResponse(
    String channelCode,
    String channelName,
    String status,
    OffsetDateTime drawTime,
    OffsetDateTime cutoffTime,
    boolean isNext,
    boolean active,
    JsonNode lastResult) {}
