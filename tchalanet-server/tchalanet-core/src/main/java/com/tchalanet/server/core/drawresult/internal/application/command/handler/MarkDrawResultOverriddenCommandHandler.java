package com.tchalanet.server.core.drawresult.internal.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.drawresult.api.command.MarkDrawResultOverriddenCommand;
import com.tchalanet.server.core.drawresult.internal.application.port.out.DrawResultWriterPort;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handler pour marquer un DrawResult comme OVERRIDDEN. Appelé suite à un événement
 * DrawResultCorrectedEvent.
 */
@UseCase
@RequiredArgsConstructor
public class MarkDrawResultOverriddenCommandHandler
    implements VoidCommandHandler<MarkDrawResultOverriddenCommand> {

  private final DrawResultWriterPort writer;

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handle(MarkDrawResultOverriddenCommand command) {
    writer.markAsOverridden(command.drawResultId(), command.reason(), command.overriddenAt());
  }
}
