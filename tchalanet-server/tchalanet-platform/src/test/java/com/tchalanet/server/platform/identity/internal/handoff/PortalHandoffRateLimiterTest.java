package com.tchalanet.server.platform.identity.internal.handoff;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.platform.identity.api.error.IdentityErrorCodes;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PortalHandoffRateLimiterTest {

  @Test
  void returnsStableCodeWhenHandoffAttemptLimitIsExceeded() {
    var limiter =
        new PortalHandoffRateLimiter(
            Clock.fixed(Instant.parse("2026-07-18T12:00:00Z"), ZoneOffset.UTC));
    var handoffId = UUID.randomUUID();

    for (int attempt = 0; attempt < 12; attempt++) {
      limiter.check("192.0.2." + attempt, handoffId);
    }

    assertThatThrownBy(() -> limiter.check("198.51.100.1", handoffId))
        .isInstanceOf(ProblemRestException.class)
        .extracting(
            error -> ((ProblemRestException) error).getProblem().getProperties().get("code"))
        .isEqualTo(IdentityErrorCodes.HANDOFF_RATE_LIMITED.code());
  }
}
