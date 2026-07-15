package com.tchalanet.server.platform.identity.internal.web.me;

import com.tchalanet.server.common.web.error.ProblemRest;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

// TODO: migrate to Redis-backed implementation for multi-instance deployment.
@Component
class PublicLoginIdentifierRateLimiter {

  private static final int MAX_IP_ATTEMPTS = 20;
  private static final int MAX_IDENTIFIER_ATTEMPTS = 8;
  private static final int MAX_GLOBAL_ATTEMPTS = 500;
  private static final long WINDOW_SECONDS = 60;

  private final Clock clock;
  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

  PublicLoginIdentifierRateLimiter() {
    this(Clock.systemUTC());
  }

  PublicLoginIdentifierRateLimiter(Clock clock) {
    this.clock = clock;
  }

  void check(HttpServletRequest request, String identifier) {
    checkKey("global", MAX_GLOBAL_ATTEMPTS);
    checkKey("ip:" + resolveClientIp(request), MAX_IP_ATTEMPTS);
    checkKey("identifier:" + normalize(identifier), MAX_IDENTIFIER_ATTEMPTS);
  }

  private void checkKey(String key, int maxAttempts) {
    var now = Instant.now(clock);
    var bucket =
        buckets.compute(
            key,
            (ignored, current) -> {
              if (current == null || now.isAfter(current.windowStart.plusSeconds(WINDOW_SECONDS))) {
                return new Bucket(now, 1);
              }
              return new Bucket(current.windowStart, current.attempts + 1);
            });
    if (bucket.attempts > maxAttempts) {
      throw ProblemRest.of(
          HttpStatus.TOO_MANY_REQUESTS,
          "auth.rate_limited",
          Map.of("retryAfterSeconds", WINDOW_SECONDS));
    }
  }

  private static String resolveClientIp(HttpServletRequest request) {
    var forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    var remoteAddr = request.getRemoteAddr();
    return remoteAddr == null || remoteAddr.isBlank() ? "unknown" : remoteAddr.trim();
  }

  private static String normalize(String value) {
    return value == null || value.isBlank() ? "blank" : value.trim().toLowerCase(Locale.ROOT);
  }

  private record Bucket(Instant windowStart, int attempts) {}
}
