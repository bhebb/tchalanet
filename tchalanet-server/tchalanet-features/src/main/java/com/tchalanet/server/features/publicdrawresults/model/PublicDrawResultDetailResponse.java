package com.tchalanet.server.features.publicdrawresults.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Réponse détail du endpoint {@code GET /public/draw-results/{drawResultId}}.
 * N'expose pas d'UUID internes, de raw payload ni de champs admin.
 *
 * <p>Le frontend construit le countdown avec {@code nextResultAt} et l'heure courante.
 */
public record PublicDrawResultDetailResponse(
    /** UUID opaque — identifiant stable et public du résultat. */
    String drawResultId,
    String slotKey,
    String provider,
    /** Clé i18n stable (ex : "draw_channel.ny.eve.label"). Utilisée par le frontend pour la traduction. */
    String drawChannelLabelKey,
    /** Label public affiché (ex : "New York — Soir"). Fallback si l'i18n n'est pas disponible. */
    String drawChannelLabel,
    LocalDate resultDate,
    LocalTime drawTime,
    String timezone,
    Instant occurredAt,
    /** Statut du tirage (PROVISIONAL, CONFIRMED, OVERRIDDEN…). */
    String status,
    /** Numéros tirés extraits de la projection haïtienne (lot1…lot4). */
    List<String> numbers,
    /** Label de la source (ex : EXTERNAL, MANUAL). */
    String sourceLabel,
    /** Heure à laquelle le résultat a été enregistré (≈ publishedAt). */
    Instant publishedAt,
    /** Prochain tirage attendu pour ce slot. */
    Instant nextResultAt) {}
