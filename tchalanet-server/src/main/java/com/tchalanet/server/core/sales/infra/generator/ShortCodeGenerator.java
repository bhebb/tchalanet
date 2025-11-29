package com.tchalanet.server.core.sales.infra.generator;

import com.tchalanet.server.core.sales.domain.ports.out.TicketPublicCodeGeneratorPort;
import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class ShortCodeGenerator implements TicketPublicCodeGeneratorPort {
  private static final String ALPHANUMERIC = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  private static final int LENGTH = 8;
  private final SecureRandom random = new SecureRandom();

  @Override
  public String generate() {
    StringBuilder sb = new StringBuilder(LENGTH);
    for (int i = 0; i < LENGTH; i++) {
      sb.append(ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length())));
    }
    return sb.toString();
  }
}
