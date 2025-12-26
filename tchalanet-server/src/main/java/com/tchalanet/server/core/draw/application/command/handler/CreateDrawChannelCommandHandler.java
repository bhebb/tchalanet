package com.tchalanet.server.core.draw.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.application.command.model.CreateDrawChannelCommand;
import com.tchalanet.server.core.draw.application.port.out.DrawChannelWriterPort;
import com.tchalanet.server.core.draw.domain.model.DrawChannel;
import com.tchalanet.server.core.draw.domain.model.DrawChannelId;
import com.tchalanet.server.core.draw.domain.model.DrawSource;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
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
    TenantId tenantId = command.tenantId();

    // parse timezone, drawTime, daysOfWeek, defaultSource with defaults
    ZoneId timezone = null;
    if (command.timezone() != null && !command.timezone().isBlank()) {
      try {
        timezone = ZoneId.of(command.timezone());
      } catch (Exception ex) {
        timezone = ZoneId.systemDefault();
      }
    } else {
      timezone = ZoneId.systemDefault();
    }

    LocalTime drawTime = null;
    if (command.drawTime() != null && !command.drawTime().isBlank()) {
      try {
        drawTime = LocalTime.parse(command.drawTime());
      } catch (DateTimeParseException ex) {
        drawTime = LocalTime.MIDNIGHT;
      }
    } else {
      drawTime = LocalTime.MIDNIGHT;
    }

    Integer cutoff = command.cutoffSec() == null ? 0 : command.cutoffSec();

    List<DayOfWeek> dow =
        command.daysOfWeek() == null
            ? List.of()
            : command.daysOfWeek().stream()
                .filter(Objects::nonNull)
                .map(s -> DayOfWeek.valueOf(s.toUpperCase(Locale.ROOT)))
                .toList();

    Integer sortOrder = command.sortOrder() == null ? 0 : command.sortOrder();

    DrawSource defaultSource;
    try {
      defaultSource =
          command.defaultSource() == null
              ? DrawSource.MANUAL
              : DrawSource.valueOf(command.defaultSource());
    } catch (IllegalArgumentException ex) {
      defaultSource = DrawSource.MANUAL;
    }

    String label = command.label() == null ? command.name() : command.label();

    DrawChannel channel =
        new DrawChannel(
            id,
            command.name(),
            label,
            tenantId,
            command.code(),
            timezone,
            drawTime,
            cutoff,
            dow,
            command.active(),
            sortOrder,
            defaultSource);

    return drawChannelWriterPort.save(channel);
  }
}
