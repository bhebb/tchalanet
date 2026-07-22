package com.tchalanet.server.core.sellerterminal.internal.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SellerTerminalSectionUpdateTest {

  @Test
  void contact_update_preserves_terminal_and_commercial_fields() {
    var terminal = terminal();

    var updated = terminal.updateContact("Grace", "Hopper", "grace@test.me", "+509", null);

    assertThat(updated.displayName()).isEqualTo("Terminal principal");
    assertThat(updated.terminalCode()).isEqualTo("POS-001");
    assertThat(updated.commissionRate()).isEqualByComparingTo("12.50");
    assertThat(updated.firstName()).isEqualTo("Grace");
    assertThat(updated.lastName()).isEqualTo("Hopper");
    assertThat(updated.email()).isEqualTo("grace@test.me");
  }

  @Test
  void terminal_label_update_preserves_contact_and_commercial_fields() {
    var terminal = terminal();

    var updated = terminal.updateTerminalLabel("Terminal Nord");

    assertThat(updated.displayName()).isEqualTo("Terminal Nord");
    assertThat(updated.firstName()).isEqualTo("Ada");
    assertThat(updated.lastName()).isEqualTo("Lovelace");
    assertThat(updated.email()).isEqualTo("ada@test.me");
    assertThat(updated.commissionRate()).isEqualByComparingTo("12.50");
  }

  private static SellerTerminal terminal() {
    return SellerTerminal.createPending(
            SellerTerminalId.of(UUID.fromString("00000000-0000-0000-0000-000000000001")),
            TenantId.of(UUID.fromString("00000000-0000-0000-0000-000000000002")),
            "POS-001",
            "Terminal principal",
            "Ada",
            "Lovelace",
            "ada@test.me",
            "+1514",
            null,
            new BigDecimal("12.50"))
        .activate(Instant.EPOCH);
  }
}
