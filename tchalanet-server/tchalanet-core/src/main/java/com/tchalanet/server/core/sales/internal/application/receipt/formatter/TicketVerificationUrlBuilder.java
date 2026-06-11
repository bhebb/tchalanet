package com.tchalanet.server.core.sales.internal.application.receipt.formatter;

import com.tchalanet.server.core.sales.api.config.TicketPublicProperties;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Builds the public ticket verification URL printed on QR codes.
 * Points to the public-facing frontend page, never to an internal API path.
 *
 * Example:
 * https://tchalanet.com/public/check-ticket?code=QVQE-NRVR
 */
@Component
@RequiredArgsConstructor
public class TicketVerificationUrlBuilder {

    private final TicketPublicProperties props;

    public String buildUrl(String publicCode) {
        String code = normalizeCode(publicCode);
        if (code.isBlank()) {
            throw new IllegalArgumentException("publicCode is required");
        }

        String base = props.baseUrl().replaceAll("/+$", "");
        String encodedCode = URLEncoder.encode(code, StandardCharsets.UTF_8);

        String path = props.ticketPathTemplate().replace("{code}", encodedCode);
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        return base + path;
    }

    private String normalizeCode(String code) {
        if (code == null) {
            return "";
        }

        return code
            .trim()
            .toUpperCase(Locale.ROOT)
            .replace(" ", "");
    }
}
