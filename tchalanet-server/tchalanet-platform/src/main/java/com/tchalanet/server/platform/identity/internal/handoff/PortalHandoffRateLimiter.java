package com.tchalanet.server.platform.identity.internal.handoff;

import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.platform.identity.api.error.IdentityErrorCodes;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
class PortalHandoffRateLimiter {
  private static final int MAX_ATTEMPTS = 12;
  private static final long WINDOW_SECONDS = 60;

  private final Clock clock;
  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

  PortalHandoffRateLimiter() {
    this(Clock.systemUTC());
  }

  PortalHandoffRateLimiter(Clock clock) {
    this.clock = clock;
  }

  void check(String clientIp, UUID handoffId) {
    checkKey("ip:" + normalize(clientIp));
    checkKey("handoff:" + handoffId);
  }

  private void checkKey(String key) {
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
    if (bucket.attempts > MAX_ATTEMPTS) {
      throw ProblemRest.of(IdentityErrorCodes.HANDOFF_RATE_LIMITED);
    }
  }

  private static String normalize(String value) {
    return value == null || value.isBlank() ? "unknown" : value.trim();
  }

  private record Bucket(Instant windowStart, int attempts) {}
}
