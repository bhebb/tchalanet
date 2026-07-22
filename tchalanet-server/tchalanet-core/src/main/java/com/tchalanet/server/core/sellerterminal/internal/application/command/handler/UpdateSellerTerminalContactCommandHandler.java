package com.tchalanet.server.core.sellerterminal.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sellerterminal.api.command.UpdateSellerTerminalContactCommand;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalReaderPort;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalWriterPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UpdateSellerTerminalContactCommandHandler
    implements CommandHandler<UpdateSellerTerminalContactCommand, Void> {

  private final SellerTerminalReaderPort reader;
  private final SellerTerminalWriterPort writer;

  @Override
  @TchTx
  public Void handle(UpdateSellerTerminalContactCommand cmd) {
    var terminal = reader.getRequired(cmd.tenantId(), cmd.sellerTerminalId());
    writer.save(
        terminal.updateContact(
            cmd.firstName(), cmd.lastName(), cmd.email(), cmd.phoneNumber(), cmd.addressId()));
    return null;
  }
}
