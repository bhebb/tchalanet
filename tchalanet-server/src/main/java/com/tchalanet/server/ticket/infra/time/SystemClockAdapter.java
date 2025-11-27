package com.tchalanet.server.ticket.infra.time;

import com.tchalanet.server.ticket.domain.ports.out.ClockPort;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class SystemClockAdapter implements ClockPort {
  @Override
  public Instant now() {
    return Instant.now();
  }
}
