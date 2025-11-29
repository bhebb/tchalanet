package com.tchalanet.server.core.draw.application.command.handler;

import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.command.model.RecordManualDrawResultCommand;
import com.tchalanet.server.core.draw.application.port.in.command.RecordManualDrawResultCommandHandler;
import com.tchalanet.server.core.draw.application.port.out.DrawReaderPort;
import com.tchalanet.server.core.draw.application.port.out.DrawResultWriterPort;
import com.tchalanet.server.core.draw.application.port.out.DrawWriterPort;
import com.tchalanet.server.core.draw.domain.model.DrawResult;
import com.tchalanet.server.core.draw.domain.model.DrawSource;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class RecordManualDrawResultUseCase implements RecordManualDrawResultCommandHandler {

  private final DrawReaderPort drawReaderPort;
  private final DrawWriterPort drawWriterPort;
  private final DrawResultWriterPort drawResultWriterPort;

  @Override
  @TchTx
  public void handle(RecordManualDrawResultCommand command) {
    var draw =
        drawReaderPort
            .findById(command.tenantId(), command.drawId())
            .orElseThrow(() -> new IllegalArgumentException("Draw not found: " + command.drawId()));

    var result =
        new DrawResult(
            DrawSource.MANUAL,
            command.numbersMain(),
            command.numbersExtra(),
            Instant.now(),
            null,
            false,
            null);

    draw.applyResult(result);

    drawResultWriterPort.save(command.tenantId(), command.drawId(), result);
    drawWriterPort.save(draw);
  }
}
