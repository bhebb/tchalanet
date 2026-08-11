package com.tchalanet.server.features.pos.tickets.app;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TicketScanResolverTest {

  private final TicketScanResolver resolver = new TicketScanResolver();

  @Test
  void resolvesPublicCodeFromPrintedVerificationUrlQueryParam() {
    assertThat(
            resolver.resolvePublicCode("https://tickets.test/public/check-ticket?code=75NR-HPD8"))
        .isEqualTo("75NRHPD8");
  }

  @Test
  void resolvesPublicCodeFromPathStyleVerificationUrl() {
    assertThat(resolver.resolvePublicCode("https://tickets.test/check/75NR-HPD8"))
        .isEqualTo("75NRHPD8");
  }

  @Test
  void resolvesRawDisplayCode() {
    assertThat(resolver.resolvePublicCode("75NR-HPD8")).isEqualTo("75NRHPD8");
  }
}
