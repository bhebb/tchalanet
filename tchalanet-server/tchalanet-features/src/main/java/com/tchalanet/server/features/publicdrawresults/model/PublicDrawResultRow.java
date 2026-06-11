package com.tchalanet.server.features.publicdrawresults.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Une ligne de la réponse paginée {@code GET /public/draw-results/history}.
 * Contient les champs suffisants pour afficher le tableau public
 * et construire le lien « Voir détail ».
 */
public record PublicDrawResultRow(
    /** UUID opaque — identifiant stable du résultat. */
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
    /** Chemin relatif vers le détail : {@code /public/results/{drawResultId}}. */
    String detailPath) {}

