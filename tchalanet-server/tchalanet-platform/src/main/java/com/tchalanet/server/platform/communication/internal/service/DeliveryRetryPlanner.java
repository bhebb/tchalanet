package com.tchalanet.server.platform.communication.internal.service;

import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class DeliveryRetryPlanner {

  public Instant nextAttempt(Instant now, int attemptNumber) {
    var minutes = Math.min(60, Math.max(1, attemptNumber * attemptNumber));
    return now.plus(Duration.ofMinutes(minutes));
  }
}
