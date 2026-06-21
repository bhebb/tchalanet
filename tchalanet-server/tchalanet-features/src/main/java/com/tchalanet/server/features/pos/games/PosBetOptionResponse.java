package com.tchalanet.server.features.pos.games;

public record PosBetOptionResponse(
    short code,
    String label,
    String description,
    String selectionHint
) {
}
