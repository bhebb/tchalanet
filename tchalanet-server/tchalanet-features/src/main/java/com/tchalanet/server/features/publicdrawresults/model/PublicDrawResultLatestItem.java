package com.tchalanet.server.features.publicdrawresults.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Un item de la réponse {@code GET /public/draw-results/latest}.
 * Représente le dernier résultat connu pour un slot public actif
 * + le prochain tirage attendu (pour le countdown frontend).
 *
 * <p>Le frontend calcule le countdown avec {@code nextResultAt - serverNow}.
 */
public record PublicDrawResultLatestItem(
    /** UUID opaque — identifiant stable du résultat ({@code null} si aucun résultat connu). */
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
    /** Instant du prochain tirage attendu pour ce slot. Source de vérité pour le countdown. */
    Instant nextResultAt,
    /** Chemin relatif vers le détail ({@code null} si pas de résultat connu). */
    String detailPath) {}

