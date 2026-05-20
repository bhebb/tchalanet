package com.tchalanet.server.features.ops.drawresult.model;

import java.time.Instant;
import java.util.List;

public record DrawResultProjectionOpsResponse(
    String id,
    String slotKey,
    Instant occurredAt,
    String lot1,
    String lot2,
    String lot3,
    String lot4,
    List<String> derivedPairs
) {}
