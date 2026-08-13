package com.tchalanet.server.core.sellerterminal.internal.application.command.handler;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.sellerterminal.api.command.UpdateSellerTerminalSettingsCommand;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalNotificationSettingsView;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalReceiptSettingsView;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalSettingsView;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalSettingsWriterPort;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateSellerTerminalSettingsCommandHandlerTest {

  private static final TenantId TENANT_ID =
      TenantId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
  private static final SellerTerminalId TERMINAL_ID =
      SellerTerminalId.of(UUID.fromString("00000000-0000-0000-0000-000000000002"));

  @Mock SellerTerminalSettingsWriterPort settingsPort;

  @Test
  void allowsTerminalSettingsUpdatesWithoutHumanActor() {
    var handler = new UpdateSellerTerminalSettingsCommandHandler(settingsPort);
    var receipt = new SellerTerminalReceiptSettingsView(true, 1, true, "AUTO", "RECEIPT_58MM", null);
    var notifications = new SellerTerminalNotificationSettingsView(true, false);
    var command =
        new UpdateSellerTerminalSettingsCommand(TENANT_ID, TERMINAL_ID, receipt, notifications, null);

    assertThatCode(() -> handler.handle(command)).doesNotThrowAnyException();

    verify(settingsPort).save(
        TENANT_ID,
        TERMINAL_ID,
        new SellerTerminalSettingsView(receipt, notifications),
        null);
  }
}
