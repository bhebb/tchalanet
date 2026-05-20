package com.tchalanet.server.features.publicdrawresults.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import tools.jackson.databind.JsonNode;

public record PublicDrawResultItemResponse(
    String slotKey,
    String provider,
    String label,
    String timezone,
    LocalTime drawTime,
    LocalDate resultDate,
    Instant occurredAt,
    String status,
    String quality,
    JsonNode haiti,
    JsonNode source) {}
