package com.tchalanet.server.core.drawresult.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.drawresult.application.command.model.MarkDrawResultOverriddenCommand;
import com.tchalanet.server.core.drawresult.application.port.out.DrawResultWriterPort;
import lombok.RequiredArgsConstructor;

/**
 * Handler pour marquer un DrawResult comme OVERRIDDEN.
 * Appelé suite à un événement DrawResultCorrectedEvent.
 */
@UseCase
@RequiredArgsConstructor
public class MarkDrawResultOverriddenCommandHandler
    implements VoidCommandHandler<MarkDrawResultOverriddenCommand> {

  private final DrawResultWriterPort writer;

  @Override
  @TchTx
  public void handle(MarkDrawResultOverriddenCommand command) {
    writer.markAsOverridden(
        command.drawResultId(),
        command.reason(),
        command.overriddenAt()
    );
  }
}

