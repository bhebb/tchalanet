package com.tchalanet.server.core.drawresult.application.view;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import tools.jackson.databind.JsonNode;

public record PublicDrawResultHistoryRowView(
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
