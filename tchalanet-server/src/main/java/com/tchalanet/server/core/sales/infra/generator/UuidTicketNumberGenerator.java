package com.tchalanet.server.core.sales.infra.generator;

import com.tchalanet.server.core.sales.application.port.out.TicketNumberGeneratorPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class UuidTicketNumberGenerator implements TicketNumberGeneratorPort {
  @Override
  public String generate() {
    return "TCK_" + UUID.randomUUID().toString().replace("-", "");
  }
}
