package com.tchalanet.server.core.sales.internal.application.receipt.formatter;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.core.sales.api.config.TicketPublicProperties;
import org.junit.jupiter.api.Test;

class TicketVerificationUrlBuilderTest {

  @Test
  void substitutesPublicCodeWhenTemplateContainsCodePlaceholder() {
    var builder =
        new TicketVerificationUrlBuilder(
            new TicketPublicProperties("https://tickets.test", "/check/{code}"));

    assertThat(builder.buildUrl("75NR-HPD8")).isEqualTo("https://tickets.test/check/75NR-HPD8");
  }

  @Test
  void appendsPublicCodeWhenTemplateOmitsCodePlaceholder() {
    var builder =
        new TicketVerificationUrlBuilder(
            new TicketPublicProperties("https://tickets.test", "/public/check-ticket"));

    assertThat(builder.buildUrl("75NR-HPD8"))
        .isEqualTo("https://tickets.test/public/check-ticket?code=75NR-HPD8");
  }

  @Test
  void preservesExistingQueryParamsWhenAppendingPublicCode() {
    var builder =
        new TicketVerificationUrlBuilder(
            new TicketPublicProperties("https://tickets.test", "/public/check-ticket?source=qr"));

    assertThat(builder.buildUrl("75NR-HPD8"))
        .isEqualTo("https://tickets.test/public/check-ticket?source=qr&code=75NR-HPD8");
  }
}
