package com.tchalanet.server.core.sellerterminal.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.sellerterminal.api.command.ChangeSellerTerminalPinCommand;
import com.tchalanet.server.core.sellerterminal.api.error.SellerTerminalErrorCodes;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalIdentityProvisionPort;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalReaderPort;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalWriterPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class ChangeSellerTerminalPinCommandHandler
    implements CommandHandler<ChangeSellerTerminalPinCommand, Void> {

  private final SellerTerminalReaderPort reader;
  private final SellerTerminalWriterPort writer;
  private final SellerTerminalIdentityProvisionPort identityProvision;

  @Override
  @TchTx
  public Void handle(ChangeSellerTerminalPinCommand cmd) {
    var terminal = reader.getRequired(cmd.tenantId(), cmd.terminalId());

    try {
      identityProvision.resetPin(cmd.terminalId(), cmd.tenantId(), cmd.newPin());
    } catch (IllegalStateException ex) {
      log.error("Firebase PIN change failed for sellerTerminal={}", cmd.terminalId().value(), ex);
      throw ProblemRest.of(SellerTerminalErrorCodes.PIN_RESET_UNAVAILABLE, java.util.Map.of(), ex);
    }

    writer.save(terminal.changePin());
    return null;
  }
}
