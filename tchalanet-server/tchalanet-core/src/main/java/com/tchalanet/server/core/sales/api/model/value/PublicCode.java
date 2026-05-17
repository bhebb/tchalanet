package com.tchalanet.server.core.sales.api.model.value;

import java.util.Locale;
import java.util.regex.Pattern;

//  court, client-facing
public record PublicCode(String value) {

    private static final Pattern PATTERN = Pattern.compile(
        "^[0-9A-HJKMNP-TV-Z]{4}-[0-9A-HJKMNP-TV-Z]{4}$"
    );

    public PublicCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("publicCode is required");
        }

        value = normalize(value);

        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("invalid publicCode format");
        }
    }

    public static PublicCode of(String value) {
        return new PublicCode(value);
    }

    public static PublicCode ofNullable(String publicCode) {
        return publicCode == null ? null : of(publicCode);
    }

    private static String normalize(String value) {
        return value
            .trim()
            .toUpperCase(Locale.ROOT)
            .replace('O', '0')
            .replace('I', '1')
            .replace('L', '1');
    }

    @Override
    public String toString() {
        return value;
    }
}
