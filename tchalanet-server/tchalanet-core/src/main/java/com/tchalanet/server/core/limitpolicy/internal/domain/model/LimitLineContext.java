package com.tchalanet.server.core.limitpolicy.internal.domain.model;

import com.tchalanet.server.catalog.game.api.model.BetType;

public record LimitLineContext(
    BetType betType,
    String selectionKey,
    long stakeCents,
    long potentialPayoutCents
) {}
