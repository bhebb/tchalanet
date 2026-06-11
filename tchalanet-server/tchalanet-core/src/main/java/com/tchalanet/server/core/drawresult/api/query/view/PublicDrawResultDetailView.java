package com.tchalanet.server.core.drawresult.api.query.view;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import tools.jackson.databind.JsonNode;

/**
 * Vue publique complète d'un draw_result identifié par son ID.
 * N'expose pas d'UUID internes ni de champs techniques admin.
 * Utilisée par l'endpoint {@code GET /public/draw-results/{drawResultId}}.
 */
public record PublicDrawResultDetailView(
    /** UUID opaque (identifiant public stable). */
    String drawResultId,
    String slotKey,
    String provider,
    /** Clé i18n stable (ex: "draw_channel.ny.eve.label"). Utilisée par le frontend pour la traduction. */
    String drawChannelLabelKey,
    /** Label public résolu côté backend (ex: "New York — Soir"). Fallback si l'i18n n'est pas disponible. */
    String drawChannelLabel,
    LocalDate resultDate,
    LocalTime drawTime,
    String timezone,
    Instant occurredAt,
    String status,
    String quality,
    JsonNode haiti,
    JsonNode source,
    /** Source type (EXTERNAL, MANUAL…) — label simplifié pour affichage. */
    String sourceLabel,
    /** Heure à laquelle le résultat a été enregistré (≈ publishedAt). */
    Instant publishedAt,
    /** Prochain tirage attendu pour ce slot. */
    Instant nextResultAt) {}

