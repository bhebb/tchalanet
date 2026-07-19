package com.tchalanet.server.core.sellerterminal.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.sellerterminal.api.command.SetSellerTerminalCommissionRateCommand;
import com.tchalanet.server.core.sellerterminal.api.error.SellerTerminalErrorCodes;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalReaderPort;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalWriterPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class SetSellerTerminalCommissionRateCommandHandler
    implements CommandHandler<SetSellerTerminalCommissionRateCommand, Void> {

  private final SellerTerminalReaderPort reader;
  private final SellerTerminalWriterPort writer;

  @Override
  @TchTx
  public Void handle(SetSellerTerminalCommissionRateCommand cmd) {
    var terminal = reader.getRequired(cmd.tenantId(), cmd.sellerTerminalId());
    try {
      writer.save(terminal.updateCommissionRate(cmd.commissionRate()));
    } catch (IllegalArgumentException ex) {
      throw ProblemRest.of(
          SellerTerminalErrorCodes.COMMISSION_RATE_INVALID, java.util.Map.of(), ex);
    }
    return null;
  }
}
