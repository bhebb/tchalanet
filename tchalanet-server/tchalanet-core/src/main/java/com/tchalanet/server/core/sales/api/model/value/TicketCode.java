package com.tchalanet.server.core.sales.api.model.value;

import java.util.Locale;
import java.util.regex.Pattern;

//  format strict interne
public record TicketCode(String value) {

    private static final Pattern PATTERN = Pattern.compile(
        "^TCK-[0-9]{6}-[0-9]{6}-[0-9A-HJKMNP-TV-Z]{6}-[0-9A-HJKMNP-TV-Z]$"
    );

    public TicketCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ticketCode is required");
        }

        value = value.trim().toUpperCase(Locale.ROOT);

        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("invalid ticketCode format");
        }
    }

    public static TicketCode of(String value) {
        return new TicketCode(value);
    }

    public static TicketCode ofNullable(String ticketCode) {
        return ticketCode == null ? null : of(ticketCode);
    }

    @Override
    public String toString() {
        return value;
    }
}
