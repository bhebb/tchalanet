package com.tchalanet.server.core.offlinesync.api.event;

import com.tchalanet.server.common.types.money.Money;

/**
 * Compact line snapshot embedded in {@link OfflineSubmissionTechValidatedEvent} so that
 * {@code core.sales} can build the ticket without querying back into {@code core.offlinesync}.
 */
public record OfflineSubmissionLineSnapshot(
    int lineNo,
    String gameCode,
    String betType,
    String betOption,
    String selectionKey,
    Money stakeAmount,
    Money potentialPayout
) {}
