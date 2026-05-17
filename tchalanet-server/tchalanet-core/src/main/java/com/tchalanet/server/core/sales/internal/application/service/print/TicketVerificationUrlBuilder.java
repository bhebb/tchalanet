package com.tchalanet.server.core.sales.internal.application.service.print;

import com.tchalanet.server.core.sales.api.config.TicketPublicProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Builds the public ticket verification URL printed on QR codes.
 * Points to the public-facing page (e.g. /ticket/{code}), never to an internal API path.
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
    String path = props.ticketPathTemplate().replace("{code}", code);
    if (!path.startsWith("/")) path = "/" + path;
    return base + path;
  }

  private String normalizeCode(String code) {
    if (code == null) return "";
    return code.trim().toUpperCase(java.util.Locale.ROOT).replace("-", "").replace(" ", "");
  }
}
