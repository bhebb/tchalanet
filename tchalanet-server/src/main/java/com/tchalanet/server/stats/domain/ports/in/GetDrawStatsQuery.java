package com.tchalanet.server.stats.domain.ports.in;

import com.tchalanet.server.stats.domain.model.DrawStats;
import java.util.Optional;
import java.util.UUID;

/** Inbound Port for querying statistics of a specific draw. */
public interface GetDrawStatsQuery {
  Optional<DrawStats> getStatsForDraw(UUID drawId);
}
