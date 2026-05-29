package com.tchalanet.server.core.limitpolicy.api.model;

import com.tchalanet.server.catalog.game.api.model.BetType;

public record LimitLineContext(
    BetType betType,
    String selectionKey,
    long stakeCents,
    long potentialPayoutCents
) {}
