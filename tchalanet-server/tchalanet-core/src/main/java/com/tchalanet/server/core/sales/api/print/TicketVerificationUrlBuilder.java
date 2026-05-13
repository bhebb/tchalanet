package com.tchalanet.server.core.sales.api.print;

import com.tchalanet.server.core.sales.internal.infra.config.TicketPublicProperties;
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
