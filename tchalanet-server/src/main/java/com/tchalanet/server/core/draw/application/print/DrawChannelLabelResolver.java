package com.tchalanet.server.core.draw.application.print;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelView;
import org.springframework.stereotype.Component;
@Component
public class DrawChannelLabelResolver {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public String resolve(DrawChannelView channel, Locale locale) {
        if (channel == null) return resolve(null, null, locale);
        // Assume channel.name() already localized by catalog/i18n layer
        return resolve(channel.name(), channel.drawTime(), locale);
    }

    public String resolve(String channelName, LocalTime drawTime, Locale locale) {
        if (channelName == null || channelName.isBlank()) return "—";

        String base = channelName.trim(); // no extra localization here
        String time = (drawTime != null) ? drawTime.format(TIME_FMT) : "";

        var sb = new StringBuilder(base);
        if (!time.isEmpty()) sb.append(" (").append(time).append(")");
        return sb.toString();
    }

    /** Short label (e.g. "Midi" / "Soir") extracted from channel name. Returns null if not found. */
    public String shortLabel(String channelName, Locale locale) {
        if (channelName == null || channelName.isBlank()) return null;

        boolean fr = locale != null && "fr".equals(locale.getLanguage());

        String norm = normalize(channelName);

        // token-ish detection with boundaries
        if (containsAnyToken(norm, "midday", "noon", "midi", "mid")) return fr ? "Midi" : "Midday";
        if (containsAnyToken(norm, "evening", "eve", "soir")) return fr ? "Soir" : "Evening";
        if (containsAnyToken(norm, "morning", "matin", "am")) return fr ? "Matin" : "Morning";
        if (containsAnyToken(norm, "afternoon", "apres-midi", "apresmidi", "pm")) return fr ? "Après-midi" : "Afternoon";

        return null;
    }

    private static String normalize(String s) {
        // lower (ROOT) + basic accent stripping for matching
        String lower = s.toLowerCase(Locale.ROOT);

        // super simple accent strip (enough for "après")
        lower = lower
            .replace("à", "a").replace("â", "a")
            .replace("é", "e").replace("è", "e").replace("ê", "e")
            .replace("î", "i")
            .replace("ô", "o")
            .replace("ù", "u").replace("û", "u")
            .replace("ç", "c");

        // normalize separators
        return lower.replace('_', ' ').replace('-', ' ');
    }

    private static boolean containsAnyToken(String text, String... tokens) {
        // cheap boundary match: split on spaces
        var parts = text.split("\\s+");
        for (var p : parts) {
            for (var t : tokens) {
                if (p.equals(t)) return true;
            }
        }
        return false;
    }
}
