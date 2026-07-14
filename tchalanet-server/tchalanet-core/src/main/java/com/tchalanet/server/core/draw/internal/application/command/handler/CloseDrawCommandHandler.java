package com.tchalanet.server.core.draw.internal.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.api.command.CloseDrawCommand;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawLifecyclePort;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawLookupPort;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class CloseDrawCommandHandler implements VoidCommandHandler<CloseDrawCommand> {

  private final DrawLookupPort drawLookupPort;
  private final DrawLifecyclePort drawLifecyclePort;
  private final Clock clock;

  @Override
  @TchTx
  public void handle(CloseDrawCommand command) {
    var drawIds = DrawLifecycleCommandGuard.requireDrawIds(command.drawIds());
    log.info("Closing {} draw(s)", drawIds.size());

    var now = clock.instant();
    for (var drawId : drawIds) {
      var draw = drawLookupPort.getById(drawId);
      draw.close(now);
      drawLifecyclePort.save(draw);
    }
  }
}
