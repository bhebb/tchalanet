package com.tchalanet.server.features.pos.tickets.app;

import java.net.URI;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class TicketScanResolver {

  public String resolvePublicCode(String scannedValue) {
    if (scannedValue == null || scannedValue.isBlank()) {
      return "";
    }
    var value = scannedValue.trim();
    if (looksLikeUrl(value)) {
      value = codeFromUrl(value);
    } else if (value.contains("=")) {
      value = value.substring(value.lastIndexOf('=') + 1);
    }
    return normalize(value);
  }

  private boolean looksLikeUrl(String value) {
    var lower = value.toLowerCase(Locale.ROOT);
    return lower.startsWith("http://") || lower.startsWith("https://");
  }

  private String codeFromUrl(String value) {
    try {
      var uri = URI.create(value);
      var queryCode = codeFromQuery(uri.getRawQuery());
      if (!queryCode.isBlank()) {
        return queryCode;
      }
      return lastPathSegment(uri.getPath(), value);
    } catch (IllegalArgumentException ignored) {
      return value;
    }
  }

  private String codeFromQuery(String query) {
    if (query == null || query.isBlank()) {
      return "";
    }
    for (var part : query.split("&")) {
      var index = part.indexOf('=');
      if (index > 0 && "code".equalsIgnoreCase(part.substring(0, index))) {
        return part.substring(index + 1);
      }
    }
    return "";
  }

  private String lastPathSegment(String path, String fallback) {
    try {
      if (path == null || path.isBlank()) {
        return fallback;
      }
      var segments = path.split("/");
      for (var i = segments.length - 1; i >= 0; i--) {
        if (!segments[i].isBlank()) {
          return segments[i];
        }
      }
      return fallback;
    } catch (IllegalArgumentException ignored) {
      return fallback;
    }
  }

  private String normalize(String value) {
    return value == null
        ? ""
        : value.trim().toUpperCase(Locale.ROOT).replace("-", "").replace(" ", "");
  }
}
