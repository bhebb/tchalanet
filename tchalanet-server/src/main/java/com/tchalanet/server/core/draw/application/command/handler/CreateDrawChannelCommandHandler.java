package com.tchalanet.server.core.draw.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.command.model.CreateDrawChannelCommand;
import com.tchalanet.server.core.draw.application.port.out.DrawChannelWriterPort;
import com.tchalanet.server.core.draw.domain.model.DrawChannel;
import com.tchalanet.server.core.draw.domain.model.DrawChannelId;
import com.tchalanet.server.core.draw.domain.model.DrawSource;
import com.tchalanet.server.core.tenant.domain.model.TenantId;
import java.time.DayOfWeek;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class CreateDrawChannelCommandHandler
    implements CommandHandler<CreateDrawChannelCommand, DrawChannel> {

  private final DrawChannelWriterPort drawChannelWriterPort;

  @Override
  public DrawChannel handle(CreateDrawChannelCommand command) {
    log.info(
        "CreateDrawChannelUseCase.handle - creating draw channel for tenant={}, code={}",
        command.tenantId(),
        command.code());
    // Build DrawChannel from command
    // Generate id, set defaults for missing fields
    DrawChannelId id = DrawChannelId.generate();
    TenantId tenantId = new TenantId(command.tenantId());
    // Assume defaults for missing fields like sortOrder = 0, defaultSource = MANUAL_HT, etc.
    DrawChannel channel =
        new DrawChannel(
            id,
            command.name(),
            command.name(), // label = name
            tenantId,
            command.gameCode(),
            command.timezone(),
            command.drawTime(),
            command.cutoffSec(),
            command.daysOfWeek().stream()
                .map(DayOfWeek::valueOf)
                .toList(), // assume daysOfWeek is List<String>
            command.active(),
            0, // sortOrder
            DrawSource.MANUAL // defaultSource
            );
    return drawChannelWriterPort.save(channel);
  }
}
