package com.tchalanet.server.features.cashier.tickets.app;

import java.net.URI;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class TicketScanResolver {

    public String resolvePublicCode(String scannedValue) {
        if (scannedValue == null || scannedValue.isBlank()) {
            return "";
        }
        var value = scannedValue.trim();
        if (looksLikeUrl(value)) {
            value = lastPathSegment(value);
        }
        if (value.contains("=")) {
            value = value.substring(value.lastIndexOf('=') + 1);
        }
        return normalize(value);
    }

    private boolean looksLikeUrl(String value) {
        var lower = value.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    private String lastPathSegment(String value) {
        try {
            var path = URI.create(value).getPath();
            if (path == null || path.isBlank()) {
                return value;
            }
            var segments = path.split("/");
            for (var i = segments.length - 1; i >= 0; i--) {
                if (!segments[i].isBlank()) {
                    return segments[i];
                }
            }
            return value;
        } catch (IllegalArgumentException ignored) {
            return value;
        }
    }

    private String normalize(String value) {
        return value == null
            ? ""
            : value.trim()
                .toUpperCase(Locale.ROOT)
                .replace("-", "")
                .replace(" ", "");
    }
}
