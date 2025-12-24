package com.tchalanet.server.core.sales.infra.generator;

import com.tchalanet.server.core.sales.application.port.out.TicketPublicCodeGeneratorPort;
import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class ShortCodeGenerator implements TicketPublicCodeGeneratorPort {
  private static final String CROCKFORD_BASE32 = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
  private static final int LENGTH = 10;
  private final SecureRandom random = new SecureRandom();

  @Override
  public String generate() {
    StringBuilder sb = new StringBuilder(LENGTH);
    for (int i = 0; i < LENGTH; i++) {
      sb.append(CROCKFORD_BASE32.charAt(random.nextInt(CROCKFORD_BASE32.length())));
    }
    return sb.toString();
  }
}
