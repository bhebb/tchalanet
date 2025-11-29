package com.tchalanet.server.core.draw.application.command.handler;

import com.tchalanet.server.common.app.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.command.model.CreateDrawCommand;
import com.tchalanet.server.core.draw.application.port.out.DrawChannelReaderPort;
import com.tchalanet.server.core.draw.application.port.out.DrawWriterPort;
import com.tchalanet.server.core.draw.domain.model.Draw;
import com.tchalanet.server.core.draw.domain.model.DrawChannel;
import com.tchalanet.server.core.draw.domain.model.DrawStatus;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class CreateDrawCommandHandler implements CommandHandler<CreateDrawCommand, Draw> {

  private final DrawWriterPort drawWriterPort;
  private final DrawChannelReaderPort drawChannelReaderPort;

  @Override
  public Draw handle(CreateDrawCommand command) {
    log.info(
        "CreateDrawCommandHandler.handle - creating draw for tenant={}, channelCode={}, scheduledDate={}",
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
    UUID drawId = UUID.randomUUID();

    // Set scheduledAt to start of day in system default zone
    ZonedDateTime scheduledAt = command.scheduledDate().atStartOfDay(ZoneId.systemDefault());

    // For cutoffAt, assume it's scheduledAt minus some time, e.g., 1 hour before. Adjust as needed.
    ZonedDateTime cutoffAt = scheduledAt.minusHours(1);

    // Create draw with SCHEDULED status
    Draw draw =
        new Draw(
            drawId,
            command.tenantId(),
            channel.id(),
            scheduledAt,
            cutoffAt,
            DrawStatus.SCHEDULED,
            null);

    // Save and return id
    var saved = drawWriterPort.save(draw);
    return saved;
  }
}
