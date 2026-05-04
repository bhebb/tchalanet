package com.tchalanet.server.core.sales.application.formatter;

import org.springframework.stereotype.Component;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class DefaultTicketDrawLabelFormatter implements TicketDrawLabelFormatter {

    private static final DateTimeFormatter TICKET_SHORT_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final DateTimeFormatter SMS_SHORT_FMT =
        DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private static final DateTimeFormatter ADMIN_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public String format(
        LocalDate drawDate,
        LocalTime drawTime,
        ZoneId zone,
        Instant scheduledAt,
        Locale locale,
        DrawLabelFormat format
    ) {
        ZonedDateTime zdt = resolve(drawDate, drawTime, zone, scheduledAt);

        if (zdt == null) {
            return "—";
        }

        return switch (format) {
            case TICKET_SHORT -> formatTicketShort(zdt, locale);
            case TICKET_FULL -> formatTicketFull(zdt, locale);
            case SMS_SHORT -> formatSmsShort(zdt, locale);
            case ADMIN_DISPLAY -> formatAdmin(zdt, locale);
            case ISO_DEBUG -> formatIso(zdt);
        };
    }

    // -----------------------
    // Résolution de la date
    // -----------------------
    private ZonedDateTime resolve(
        LocalDate drawDate,
        LocalTime drawTime,
        ZoneId zone,
        Instant scheduledAt
    ) {
        if (drawDate != null && drawTime != null && zone != null) {
            return ZonedDateTime.of(drawDate, drawTime, zone);
        }

        if (scheduledAt != null) {
            ZoneId resolvedZone = zone != null ? zone : ZoneId.systemDefault();
            return scheduledAt.atZone(resolvedZone);
        }

        return null;
    }

    // -----------------------
    // Formats
    // -----------------------

    private String formatTicketShort(ZonedDateTime zdt, Locale locale) {
        return TICKET_SHORT_FMT.withLocale(locale).format(zdt);
    }

    private String formatTicketFull(ZonedDateTime zdt, Locale locale) {
        return "Tirage du " +
            TICKET_SHORT_FMT.withLocale(locale).format(zdt) +
            " (" + zdt.getZone().getId() + ")";
    }

    private String formatSmsShort(ZonedDateTime zdt, Locale locale) {
        return SMS_SHORT_FMT.withLocale(locale).format(zdt);
    }

    private String formatAdmin(ZonedDateTime zdt, Locale locale) {
        return ADMIN_FMT.withLocale(locale).format(zdt)
            + " (" + zdt.getZone().getId() + ")";
    }

    private String formatIso(ZonedDateTime zdt) {
        return zdt.toInstant().toString();
    }
}
