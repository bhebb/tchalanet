package com.tchalanet.server.core.sales.application.formatter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Locale;

public interface TicketDrawLabelFormatter {

    String format(
        LocalDate drawDate,
        LocalTime drawTime,
        ZoneId zone,
        Instant scheduledAt,
        Locale locale,
        DrawLabelFormat format
    );
}
