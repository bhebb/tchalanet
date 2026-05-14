package com.tchalanet.server.catalog.game.api.model;

import com.tchalanet.server.common.types.id.GameId;
import java.time.Instant;

/**
 * Immutable view for Game catalog (reference data).
 * Maps to spec requirement G4 (mapping boundaries).
 * Exposed by GameCatalog API (catalog/game/api).
 */
public record GameView(
    GameId id,
    String code,
    String name,
    String category,
    String combination,
    int minDigits,
    int maxDigits,
    String description,
    boolean active,
    int sortOrder,
    Instant createdAt,
    Instant updatedAt
) {}
