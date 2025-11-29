package com.tchalanet.server.common.time;

import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class SystemClockAdapter implements ClockPort {
  @Override
  public Instant now() {
    return Instant.now();
  }
}
