package com.tchalanet.server.core.limitpolicy.domain.model;

import com.tchalanet.server.common.types.enums.BetType;

public record LimitLineContext(
    BetType betType,
    String selectionKey,
    long stakeCents,
    long potentialPayoutCents
) {}
