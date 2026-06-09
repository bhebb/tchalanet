package com.tchalanet.server.core.drawresult.api.query.view;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import tools.jackson.databind.JsonNode;

public record PublicDrawResultHistoryRowView(
    String slotKey,
    String provider,
    /** Clé i18n stable (ex: "draw_channel.ny.eve.label"). Utilisée par le frontend pour la traduction. */
    String drawChannelLabelKey,
    /** Label public résolu côté backend (ex: "New York — Soir"). Fallback si l'i18n n'est pas disponible. */
    String label,
    String timezone,
    LocalTime drawTime,
    LocalDate resultDate,
    Instant occurredAt,
    String status,
    String quality,
    JsonNode haiti,
    JsonNode source,
    /** UUID opaque — identifiant public du draw_result. */
    String drawResultId) {}
