package com.tchalanet.server.draw.infra.time;

import com.tchalanet.server.draw.domain.ports.ClockPort;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class SystemClockAdapter implements ClockPort {
  @Override
  public Instant now() {
    return Instant.now();
  }
}
