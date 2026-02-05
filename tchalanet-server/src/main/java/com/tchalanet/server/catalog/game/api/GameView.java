package com.tchalanet.server.catalog.game.api;

import com.tchalanet.server.common.types.id.GameId;

/** Read-only view of a global Game (cache-friendly). */
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
    int sortOrder) {}
