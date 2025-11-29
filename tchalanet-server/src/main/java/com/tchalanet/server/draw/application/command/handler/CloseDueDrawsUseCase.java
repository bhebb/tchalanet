package com.tchalanet.server.draw.application.command.handler;

import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.draw.application.port.in.command.CloseDueDrawsCommandHandler;
import com.tchalanet.server.draw.application.port.out.DrawReaderPort;
import com.tchalanet.server.draw.application.port.out.DrawWriterPort;
import com.tchalanet.server.draw.domain.model.Draw;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class CloseDueDrawsUseCase implements CloseDueDrawsCommandHandler {

  private final DrawReaderPort drawReaderPort;
  private final DrawWriterPort drawWriterPort;
  private final Clock clock;

  @Override
  @TchTx
  public void handle(UUID tenantId) {
    var now = ZonedDateTime.now(clock);
    var draws = drawReaderPort.findClosableDraws(tenantId, now);

    // logique métier dans l’agrégat
    draws.forEach(Draw::close);

    drawWriterPort.saveAll(draws);
  }
}
