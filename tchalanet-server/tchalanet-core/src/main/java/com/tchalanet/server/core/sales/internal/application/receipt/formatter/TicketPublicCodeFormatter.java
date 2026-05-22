package com.tchalanet.server.core.sales.internal.application.receipt.formatter;

import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class TicketPublicCodeFormatter {

    public String normalize(String code) {
        if (code == null) {
            return "";
        }
        return code.trim()
            .toUpperCase(Locale.ROOT)
            .replace("-", "")
            .replace(" ", "");
    }

    public String display(String code) {
        var normalized = normalize(code);
        if (normalized.length() <= 4) {
            return normalized;
        }
        return normalized.substring(0, 4) + "-" + normalized.substring(4);
    }
}
