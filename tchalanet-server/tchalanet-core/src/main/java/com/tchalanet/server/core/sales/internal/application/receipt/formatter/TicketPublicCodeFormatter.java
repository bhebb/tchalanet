package com.tchalanet.server.core.sales.internal.application.receipt.formatter;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class TicketPublicCodeFormatter {

    public String normalize(String code) {
        if (code == null) {
            return "";
        }
        return code.trim()
            .toUpperCase(Locale.ROOT)
            .replace("-", "")
            .replace(" ", "")
            .trim();
    }

    public String display(String code) {
        var normalized = normalize(code);
        if (normalized.length() <= 4) {
            return normalized;
        }
        return normalized.substring(0, 4) + "-" + normalized.substring(4);
    }
}
