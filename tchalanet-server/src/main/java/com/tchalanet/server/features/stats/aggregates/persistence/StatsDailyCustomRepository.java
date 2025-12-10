package com.tchalanet.server.features.stats.aggregates.persistence;

import java.time.LocalDate;
import java.util.UUID;

public interface StatsDailyCustomRepository {

    void upsertAndIncrement(
            String dimensionType,
            UUID dimensionId,
            LocalDate refDate,
            long ticketsDelta,
            long cancelledDelta,
            long stakeDelta,
            long winningsDelta,
            long payoutsDelta,
            long sessionsOpenedDelta,
            long sessionsClosedDelta
    );
}

