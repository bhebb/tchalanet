package com.tchalanet.server.draw.domain.ports;

import java.time.Instant;

/**
 * A port to abstract the concept of current time. This is crucial for making business logic
 * testable.
 */
public interface ClockPort {
  Instant now();
}
