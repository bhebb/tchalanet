package com.tchalanet.server.core.sellerterminal.internal.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.sellerterminal.api.command.UpdateSellerTerminalSettingsCommand;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalNotificationSettingsView;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalReceiptSettingsView;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class UpdateSellerTerminalSettingsCommandHandlerTest {

  private static final TenantId TENANT_ID =
      TenantId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
  private static final SellerTerminalId TERMINAL_ID =
      SellerTerminalId.of(UUID.fromString("00000000-0000-0000-0000-000000000002"));

  @Test
  void allowsTerminalSettingsUpdatesWithoutHumanActor() {
    var jdbc = new CapturingJdbcTemplate();
    var handler = new UpdateSellerTerminalSettingsCommandHandler(jdbc);
    var command =
        new UpdateSellerTerminalSettingsCommand(
            TENANT_ID,
            TERMINAL_ID,
            new SellerTerminalReceiptSettingsView(true, 1, true, "AUTO", "RECEIPT_58MM", null),
            new SellerTerminalNotificationSettingsView(true, false),
            null);

    assertThatCode(() -> handler.handle(command)).doesNotThrowAnyException();

    assertThat(jdbc.args()).hasSize(12);
    assertThat(jdbc.args()[10]).isNull();
    assertThat(jdbc.args()[11]).isNull();
  }

  private static final class CapturingJdbcTemplate extends JdbcTemplate {

    private Object[] args = new Object[0];

    @Override
    public int update(String sql, Object... args) {
      this.args = args;
      return 1;
    }

    Object[] args() {
      return args;
    }
  }
}
