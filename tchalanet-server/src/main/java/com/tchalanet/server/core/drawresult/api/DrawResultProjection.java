package com.tchalanet.server.core.drawresult.api;

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
    String pick3,
    List<String> twoDigits) {}
