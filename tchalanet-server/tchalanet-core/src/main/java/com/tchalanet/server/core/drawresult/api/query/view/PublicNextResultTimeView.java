package com.tchalanet.server.core.drawresult.api.query.view;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record PublicNextResultTimeView(
    Instant expectedAt,
    LocalDate localDate,
    LocalTime localTime,
    String timezone,
    long countdownSeconds,
    String status
) {}
