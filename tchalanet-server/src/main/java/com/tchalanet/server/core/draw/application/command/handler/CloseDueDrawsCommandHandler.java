package com.tchalanet.server.core.draw.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.command.model.CloseDueDrawsCommand;
import com.tchalanet.server.core.draw.application.port.out.DrawReaderPort;
import com.tchalanet.server.core.draw.application.port.out.DrawWriterPort;
import com.tchalanet.server.core.draw.domain.model.Draw;
import java.time.Clock;
import java.time.ZonedDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class CloseDueDrawsCommandHandler implements VoidCommandHandler<CloseDueDrawsCommand> {

  private final DrawReaderPort drawReaderPort;
  private final DrawWriterPort drawWriterPort;
  private final Clock clock;

  @Override
  @TchTx
  public void handle(CloseDueDrawsCommand command) {
    var now = ZonedDateTime.now(clock);
    var draws = drawReaderPort.findClosableDraws(command.tenantId(), now);

    // logique métier dans l’agrégat
    draws.forEach(Draw::close);

    drawWriterPort.saveAll(draws);
  }
}
