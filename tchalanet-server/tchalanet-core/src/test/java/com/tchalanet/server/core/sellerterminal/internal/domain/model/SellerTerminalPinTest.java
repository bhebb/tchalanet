package com.tchalanet.server.core.sellerterminal.internal.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SellerTerminalPinTest {

  @Test
  void changePinClearsTheMandatoryPinChangeGate() {
    var terminal =
        SellerTerminal.createPending(
                SellerTerminalId.of(UUID.fromString("00000000-0000-0000-0000-000000000001")),
                TenantId.of(UUID.fromString("00000000-0000-0000-0000-000000000002")),
                "POS-001",
                "Terminal principal",
                "Terminal",
                "Principal",
                "terminal@example.test",
                null,
                null,
                BigDecimal.TEN)
            .activate(Instant.EPOCH)
            .resetPin(Instant.parse("2026-07-20T00:00:00Z"));

    assertThat(terminal.mustChangePin()).isTrue();
    assertThat(terminal.changePin().mustChangePin()).isFalse();
  }
}
