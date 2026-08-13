package com.tchalanet.server.core.sellerterminal.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sellerterminal.api.command.UpdateSellerTerminalSettingsCommand;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalSettingsView;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalSettingsWriterPort;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UpdateSellerTerminalSettingsCommandHandler
    implements CommandHandler<UpdateSellerTerminalSettingsCommand, Void> {

  private static final List<String> PRINTER_MODES = List.of("AUTO", "POS_DIRECT", "SYSTEM_PDF");
  private static final List<String> PAPER_SIZES = List.of("RECEIPT_58MM", "RECEIPT_80MM");

  private final SellerTerminalSettingsWriterPort settingsPort;

  @Override
  @TchTx
  public Void handle(UpdateSellerTerminalSettingsCommand command) {
    validate(command);
    settingsPort.save(
        command.tenantId(),
        command.sellerTerminalId(),
        new SellerTerminalSettingsView(command.receipt(), command.notifications()),
        command.actorUserId());
    return null;
  }

  private static void validate(UpdateSellerTerminalSettingsCommand command) {
    if (!PRINTER_MODES.contains(command.receipt().printerMode())) {
      throw new IllegalArgumentException("Unsupported receipt printer mode");
    }
    if (!PAPER_SIZES.contains(command.receipt().paperSize())) {
      throw new IllegalArgumentException("Unsupported receipt paper size");
    }
  }
}
