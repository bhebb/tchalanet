package com.tchalanet.server.features.publicdrawresults.model;

import java.time.Instant;
import java.util.List;

/**
 * Réponse du endpoint {@code GET /public/draw-results/latest}.
 * Alimente le widget « Derniers tirages » de la home publique.
 *
 * <p>Le frontend calcule le countdown avec {@code item.nextResultAt() - serverNow}.
 */
public record PublicDrawResultLatestResponse(
    List<PublicDrawResultLatestItem> items,
    /** Instant serveur au moment de la réponse — source de vérité pour le countdown. */
    Instant serverNow) {}
