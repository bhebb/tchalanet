package com.tchalanet.server.core.sales.api.model.value;

import java.util.Locale;
import java.util.regex.Pattern;

//court, possession/proof

public record VerificationCode(String value) {

    private static final Pattern PATTERN = Pattern.compile(
        "^[0-9A-HJKMNP-TV-Z]{4}-[0-9A-HJKMNP-TV-Z]{4}$"
    );

    public VerificationCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("verificationCode is required");
        }

        value = normalize(value);

        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("invalid verificationCode format");
        }
    }

    public static VerificationCode of(String value) {
        return new VerificationCode(value);
    }

    public static VerificationCode ofNullable(String verificationCode) {
        return verificationCode == null ? null : of(verificationCode);
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
