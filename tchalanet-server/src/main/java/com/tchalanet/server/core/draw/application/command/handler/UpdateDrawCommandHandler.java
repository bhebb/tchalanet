package com.tchalanet.server.core.draw.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.command.model.UpdateDrawCommand;
import com.tchalanet.server.core.draw.application.port.out.DrawReaderPort;
import com.tchalanet.server.core.draw.application.port.out.DrawWriterPort;
import com.tchalanet.server.core.draw.domain.model.Draw;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class UpdateDrawCommandHandler implements VoidCommandHandler<UpdateDrawCommand> {

  private final DrawReaderPort drawReaderPort;
  private final DrawWriterPort drawWriterPort;

  @Override
  public void handle(UpdateDrawCommand command) {
    log.info(
        "UpdateDrawCommandHandler.handle - updating draw tenant={}, drawId={}, newScheduledDate={}",
        command.tenantId(),
        command.drawId(),
        command.scheduledDate());

    // Find the existing draw
    Draw existing =
        drawReaderPort
            .findById(command.drawId())
            .orElseThrow(() -> new IllegalArgumentException("Draw not found: " + command.drawId()));

    // Update scheduledAt
    ZonedDateTime newScheduledAt = command.scheduledDate().atStartOfDay(ZoneId.systemDefault());
    ZonedDateTime newCutoffAt = newScheduledAt.minusHours(1); // Adjust as needed

    // Reschedule the draw using domain method
    existing.reschedule(newScheduledAt, newCutoffAt);

    // Save the updated draw
    drawWriterPort.save(existing);
  }
}
