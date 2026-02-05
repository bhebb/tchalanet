package com.tchalanet.server.catalog.game.api;

import com.tchalanet.server.common.types.id.GameId;
import java.time.Instant;

/** Lightweight summary view of a Game for lists/pickers. */
public record GameSummaryView(
    GameId id, String code, String name, boolean active, Instant updatedAt) {}
