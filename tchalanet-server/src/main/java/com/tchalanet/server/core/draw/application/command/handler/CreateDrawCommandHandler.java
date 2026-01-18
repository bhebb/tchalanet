package com.tchalanet.server.core.draw.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.draw.application.command.model.CreateDrawCommand;
import com.tchalanet.server.core.draw.application.port.out.DrawChannelReaderPort;
import com.tchalanet.server.core.draw.application.port.out.DrawLifecyclePort;
import com.tchalanet.server.core.draw.domain.model.Draw;
import com.tchalanet.server.core.draw.domain.model.DrawChannel;
import com.tchalanet.server.core.draw.domain.model.DrawStatus;
import com.tchalanet.server.catalog.drawresult.domain.model.DrawSource;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class CreateDrawCommandHandler implements CommandHandler<CreateDrawCommand, DrawId> {

  private final DrawLifecyclePort drawWriterPort;
  private final DrawChannelReaderPort drawChannelReaderPort;

  @Override
  public DrawId handle(CreateDrawCommand command) {
    log.info(
        "CreateDrawCommandHandler.handle - creating draw for tenant={}, drawChannelCode={}, scheduledDate={}",
        command.tenantId(),
        command.channelCode(),
        command.scheduledDate());

    // Find the draw channel
    DrawChannel channel =
        drawChannelReaderPort
            .findByCode(command.tenantId(), command.channelCode())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Draw channel not found: " + command.channelCode()));

    // Generate draw id
    DrawId drawId = DrawId.random();

    // Set scheduledAt to start of day in system default zone
    ZonedDateTime scheduledAt = command.scheduledDate().atStartOfDay(ZoneId.systemDefault());

    // For cutoffAt, assume it's scheduledAt minus some time, e.g., 1 hour before. Adjust as needed.
    ZonedDateTime cutoffAt = scheduledAt.minusHours(1);

    // Create draw with SCHEDULED status
    Draw draw =
        new Draw(
            drawId,
            command.tenantId(),
            channel,
            scheduledAt,
            cutoffAt,
            DrawStatus.SCHEDULED,
            DrawSource.SYSTEM,
            null);

    // Save and return id
    var saved = drawWriterPort.save(draw);
    return saved.id();
  }
}
