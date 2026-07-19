package com.tchalanet.server.core.sellerterminal.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.sellerterminal.api.command.BlockSellerTerminalCommand;
import com.tchalanet.server.core.sellerterminal.api.error.SellerTerminalErrorCodes;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalReaderPort;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalWriterPort;
import com.tchalanet.server.core.sellerterminal.internal.domain.model.SellerTerminalStatusException;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class BlockSellerTerminalCommandHandler
    implements CommandHandler<BlockSellerTerminalCommand, Void> {

  private final SellerTerminalReaderPort reader;
  private final SellerTerminalWriterPort writer;
  private final Clock clock;

  @Override
  @TchTx
  public Void handle(BlockSellerTerminalCommand cmd) {
    var terminal = reader.getRequired(cmd.tenantId(), cmd.terminalId());
    final var blocked;
    try {
      blocked = terminal.block(cmd.actorUserId(), cmd.reason(), Instant.now(clock));
    } catch (SellerTerminalStatusException ex) {
      throw ProblemRest.of(
          SellerTerminalErrorCodes.STATUS_TRANSITION_INVALID, java.util.Map.of(), ex);
    }
    writer.save(blocked);
    return null;
  }
}
