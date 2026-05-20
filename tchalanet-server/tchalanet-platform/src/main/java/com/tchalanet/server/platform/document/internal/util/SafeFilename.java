package com.tchalanet.server.platform.document.internal.util;

import java.text.Normalizer;

public final class SafeFilename {

    private SafeFilename() {}

    public static String of(String raw, String fallback) {
        String value = raw == null || raw.isBlank() ? fallback : raw;

        value = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "");

        value = value
            .replaceAll("[^a-zA-Z0-9._-]", "_")
            .replaceAll("_+", "_")
            .replaceAll("^_+", "")
            .replaceAll("_+$", "");

        if (value.isBlank()) {
            value = fallback == null || fallback.isBlank() ? "document" : fallback;
        }

        return value.length() > 80 ? value.substring(0, 80) : value;
    }
}
