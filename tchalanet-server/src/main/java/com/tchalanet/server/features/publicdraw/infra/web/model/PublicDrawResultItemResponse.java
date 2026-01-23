package com.tchalanet.server.features.publicdraw.infra.web.model;

import com.tchalanet.server.common.types.enums.ResultQuality;
import com.tchalanet.server.core.drawresult.domain.model.DrawResultStatus;
import java.time.Instant;
import java.time.LocalDate;

public record PublicDrawResultItemResponse(
    String slotKey,
    String provider,
    String timezone,
    String drawTime, // "14:30"
    LocalDate drawDate, // 2026-01-10 (date locale slot)
    Instant occurredAt, // UTC
    String lot1,
    String lot2,
    String lot3,
    String lot4,
    DrawResultStatus status,
    ResultQuality quality,
    String source) {}
