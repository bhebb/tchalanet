package com.tchalanet.server.core.draw.application.command.handler;

import com.tchalanet.server.catalog.drawchannel.api.DrawChannelCatalog;
import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelView;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.enums.DrawSource;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.draw.application.command.model.CreateDrawCommand;
import com.tchalanet.server.core.draw.application.port.out.DrawLifecyclePort;
import com.tchalanet.server.core.draw.domain.model.Draw;
import com.tchalanet.server.core.draw.domain.model.DrawStatus;
import com.tchalanet.server.core.draw.domain.model.DrawSummary;
import com.tchalanet.server.common.time.OccurredAtResolver;
import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class CreateDrawCommandHandler implements CommandHandler<CreateDrawCommand, DrawSummary> {

  private final DrawLifecyclePort drawWriterPort;
  private final DrawChannelCatalog drawChannelCatalog;
  private final ResultSlotCatalog resultSlotCatalog;
  private final IdGenerator idGenerator;
  private final Clock clock;

  @Override
  public DrawSummary handle(CreateDrawCommand command) {
    log.info(
        "CreateDrawCommandHandler.handle - creating draw for tenant={}, drawChannelCode={}, scheduledDate={}",
        command.tenantId(),
        command.channelCode(),
        command.scheduledDate());

    // Find the draw channel
    DrawChannelView channel =
        drawChannelCatalog
            .findByTenantAndCode(command.tenantId(), command.channelCode())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Draw channel not found: " + command.channelCode()));

    // [D3] Find result slot to get correct timezone and draw time
    ResultSlotView slot = resultSlotCatalog.findById(channel.resultSlotId())
        .orElseThrow(() -> new IllegalArgumentException("Result slot not found: " + channel.resultSlotId()));

    // Generate draw id
    DrawId drawId = DrawId.of(idGenerator.newUuid());

    // [D3] Use OccurredAtResolver for correct scheduledAt calculation
    Instant scheduledAtInstant = OccurredAtResolver.resolve(
        null, command.scheduledDate(), slot.drawTime(), slot.timezone(), clock);

    ZonedDateTime scheduledAt = ZonedDateTime.ofInstant(scheduledAtInstant, slot.timezone());

    // [D3] Use channel cutoffSec for cutoffAt calculation
    int cutoffSec = channel.cutoffSec() != null ? channel.cutoffSec() : 300;
    ZonedDateTime cutoffAt = scheduledAt.minusSeconds(cutoffSec);

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

    drawWriterPort.save(draw);

    return new DrawSummary(
        drawId,
        channel.code(),
        channel.name(),
        scheduledAt,
        cutoffAt,
        DrawStatus.SCHEDULED,
        false,
        channel.active(),
        List.of());
  }
}
