package com.tchalanet.server.core.sales.infra.generator;

import com.tchalanet.server.core.sales.domain.ports.out.TicketNumberGeneratorPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class UuidTicketNumberGenerator implements TicketNumberGeneratorPort {
  @Override
  public String generate() {
    // In a real system, this would be a KSUID, ULID, or other sortable unique ID.
    return UUID.randomUUID().toString();
  }
}
