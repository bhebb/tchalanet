package com.tchalanet.server.features.publicdrawresults.model;

import java.util.List;

/**
 * Réponse paginée du endpoint {@code GET /public/draw-results/history}.
 * Alimente la page publique {@code /public/results}.
 */
public record PublicDrawResultHistoryResponse(
    List<PublicDrawResultRow> items,
    int page,
    int size,
    long totalItems,
    int totalPages) {}
