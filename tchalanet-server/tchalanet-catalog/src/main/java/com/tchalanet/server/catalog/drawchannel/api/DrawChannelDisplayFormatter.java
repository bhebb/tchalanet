package com.tchalanet.server.catalog.drawchannel.api;

import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelView;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Formats draw channel display labels for UI/print.
 * Replaces legacy DrawChannelLabelResolver from core.draw.
 *
 * <p>This formatter now uses structured {@code draw_channel.period} field
 * instead of parsing {@code shortLabel()} from channel name.
 */
@Component
public class DrawChannelDisplayFormatter {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Resolves full channel label from DrawChannelView.
     * Format: "Channel Name (HH:mm)"
     *
     * @param channel The draw channel view
     * @param locale The display locale
     * @return Formatted channel label
     */
    public String resolve(DrawChannelView channel, Locale locale) {
        if (channel == null) {
            return resolve(null, null, locale);
        }
        // Assume channel.name() already localized by catalog/i18n layer
        return resolve(channel.name(), channel.drawTime(), locale);
    }

    /**
     * Resolves channel label from raw name and draw time.
     *
     * @param channelName The channel name
     * @param drawTime The draw time
     * @param locale The display locale
     * @return Formatted channel label
     */
    public String resolve(String channelName, LocalTime drawTime, Locale locale) {
        if (channelName == null || channelName.isBlank()) {
            return "—";
        }

        String base = channelName.trim();
        String time = (drawTime != null) ? drawTime.format(TIME_FMT) : "";

        var sb = new StringBuilder(base);
        if (!time.isEmpty()) {
            sb.append(" (").append(time).append(")");
        }
        return sb.toString();
    }

    /**
     * Resolves period label from structured period field.
     * Uses {@code draw_channel.period} instead of parsing channel name.
     *
     * @param period The structured period value (e.g., MORNING, MIDDAY, EVENING)
     * @param locale The display locale
     * @return Localized period label
     */
    public String resolvePeriod(String period, Locale locale) {
        if (period == null || period.isBlank()) {
            return null;
        }

        boolean fr = locale != null && "fr".equals(locale.getLanguage());

        return switch (period.toUpperCase(Locale.ROOT)) {
            case "MIDDAY", "NOON", "MIDI" -> fr ? "Midi" : "Midday";
            case "EVENING", "SOIR" -> fr ? "Soir" : "Evening";
            case "MORNING", "MATIN" -> fr ? "Matin" : "Morning";
            case "AFTERNOON", "APRES-MIDI" -> fr ? "Après-midi" : "Afternoon";
            default -> period; // fallback to raw value
        };
    }

    /**
     * Resolves period label from DrawChannelView.
     *
     * @param channel The draw channel view
     * @param locale The display locale
     * @return Localized period label
     */
    public String resolvePeriod(DrawChannelView channel, Locale locale) {
        if (channel == null) {
            return null;
        }
        return resolvePeriod(channel.period(), locale);
    }
}

