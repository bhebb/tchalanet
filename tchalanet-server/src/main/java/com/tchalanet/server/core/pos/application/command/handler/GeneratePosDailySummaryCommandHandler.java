package com.tchalanet.server.core.pos.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.pos.application.command.model.GeneratePosDailySummaryCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class GeneratePosDailySummaryCommandHandler implements VoidCommandHandler<GeneratePosDailySummaryCommand> {

  @Override
  public void handle(GeneratePosDailySummaryCommand command) {
    // TODO: generate and persist daily summary for POS
    throw new UnsupportedOperationException("GeneratePosDailySummaryCommandHandler not implemented yet");
  }
}

