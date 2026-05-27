package com.tchalanet.server.core.sales.internal.application.receipt.formatter;

import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptI18nKeys;
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

    private final TicketReceiptI18nResolver i18nResolver;

    public DefaultTicketDrawLabelFormatter(TicketReceiptI18nResolver i18nResolver) {
        this.i18nResolver = i18nResolver;
    }

    @Override
    public String format(
        String drawChannelName,
        LocalDate drawDate,
        LocalTime drawTime,
        ZoneId zone,
        Instant scheduledAt,
        Locale locale,
        DrawLabelFormat format
    ) {
        ZonedDateTime zdt = resolve(drawDate, drawTime, zone, scheduledAt);
        var translations = i18nResolver.resolve(locale, null);
        String channel = normalizeChannel(drawChannelName, translations);

        if (zdt == null) {
            return channel;
        }

        return switch (format) {
            case TICKET_SHORT -> channel + " - " + formatTicketShort(zdt, locale);
            case TICKET_FULL -> channel + " - " + formatTicketFull(zdt, locale, translations);
            case SMS_SHORT -> channel + " - " + formatSmsShort(zdt, locale);
            case ADMIN_DISPLAY -> formatAdmin(zdt, locale);
            case ISO_DEBUG -> formatIso(zdt);
        };
    }

    private ZonedDateTime resolve(
        LocalDate drawDate,
        LocalTime drawTime,
        ZoneId zone,
        Instant scheduledAt
    ) {
        if (drawDate != null && drawTime != null && zone != null) {
            return ZonedDateTime.of(drawDate, drawTime, zone);
        }

        if (scheduledAt != null && zone != null) {
            return scheduledAt.atZone(zone);
        }

        return null;
    }

    private String normalizeChannel(
        String drawChannelName,
        TicketReceiptI18nResolver.TicketReceiptTranslations translations
    ) {
        if (drawChannelName == null || drawChannelName.isBlank()) {
            return translations.text(TicketReceiptI18nKeys.DRAW_SECTION);
        }
        return drawChannelName.trim();
    }

    private String formatTicketShort(ZonedDateTime zdt, Locale locale) {
        return TICKET_SHORT_FMT.withLocale(locale).format(zdt);
    }

    private String formatTicketFull(
        ZonedDateTime zdt,
        Locale locale,
        TicketReceiptI18nResolver.TicketReceiptTranslations translations
    ) {
        return translations.text(TicketReceiptI18nKeys.DRAW_FULL_PREFIX) + " " +
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
