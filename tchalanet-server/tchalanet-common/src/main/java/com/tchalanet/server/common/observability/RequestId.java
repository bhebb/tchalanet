package com.tchalanet.server.common.observability;

import java.util.regex.Pattern;

public record RequestId(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[A-Za-z0-9._:\\-]{8,96}$");

    public static boolean isValid(String value) {
        return value != null && !value.isBlank() && PATTERN.matcher(value).matches();
    }

    @Override
    public String toString() {
        return value;
    }
}
