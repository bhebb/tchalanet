package com.tchalanet.server.core.sales.internal.application.formatter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Locale;

public interface TicketDrawLabelFormatter {

    String format(
        String drawChannelName,
        LocalDate drawDate,
        LocalTime drawTime,
        ZoneId zone,
        Instant scheduledAt,
        Locale locale,
        DrawLabelFormat format
    );
}
