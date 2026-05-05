package com.tchalanet.server.core.drawresult.application.port.out;

import com.tchalanet.server.common.types.id.DrawResultId;

import java.time.Instant;
import java.util.List;

public record DrawResultProjection(
    DrawResultId id,
    String slotKey,
    Instant occurredAt,
    String lot1,
    String lot2,
    String lot3,
    String lot4,
    List<String> derivedPairs
) {
}
