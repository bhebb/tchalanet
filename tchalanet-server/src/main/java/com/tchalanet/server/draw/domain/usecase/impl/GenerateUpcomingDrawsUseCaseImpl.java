package com.tchalanet.server.draw.domain.usecase.impl;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.draw.domain.model.Draw;
import com.tchalanet.server.draw.domain.model.DrawChannel;
import com.tchalanet.server.draw.domain.ports.DrawChannelRepository;
import com.tchalanet.server.draw.domain.ports.DrawRepository;
import com.tchalanet.server.draw.domain.usecase.GenerateUpcomingDrawsUseCase;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class GenerateUpcomingDrawsUseCaseImpl implements GenerateUpcomingDrawsUseCase {

  private final DrawChannelRepository channelRepo;
  private final DrawRepository drawRepo;

  @Override
  public void generateForNextDays(int days) {
    log.info("Starting generation of upcoming draws for the next {} days.", days);

    List<DrawChannel> channels = channelRepo.findAllActive();
    log.debug("Found {} active draw channels to process.", channels.size());

    for (DrawChannel channel : channels) {
      try {
        ZoneId zone = ZoneId.of(channel.timezone());
        LocalDate startDate = LocalDate.now(zone);

        for (int i = 0; i < days; i++) {
          LocalDate drawDate = startDate.plusDays(i);

          if (!channel.isScheduledOn(drawDate.getDayOfWeek())) {
            continue;
          }

          LocalTime drawTime = channel.drawTime();
          Instant scheduledAt = ZonedDateTime.of(drawDate, drawTime, zone).toInstant();

          // Use the port interface directly, without casting to implementation
          boolean alreadyExists =
              drawRepo.existsByTenantChannelAndScheduledAt(
                  channel.tenantId().value(), channel.id().value(), scheduledAt);

          if (alreadyExists) {
            log.trace("Draw already exists for channel {} on {}", channel.code(), scheduledAt);
            continue;
          }

          // Use the rich domain model's factory method to create the draw
          // todo test
          Draw newDraw = null; // createdBy - This should be the ID of the system user/job
          /*Draw newDraw = Draw.sc(
              UUID.randomUUID(),
              channel.tenantId().value(),
              channel.id().value(),
              channel.gameCode(),
              scheduledAt,
              channel.cutoffSec(),
              null // createdBy - This should be the ID of the system user/job
          );*/

          drawRepo.save(newDraw);
          log.debug("Created new draw for channel {} at {}", channel.code(), scheduledAt);
        }
      } catch (Exception e) {
        log.error("Failed to generate draws for channel {}: {}", channel.code(), e.getMessage(), e);
      }
    }

    log.info("Finished generation of upcoming draws.");
  }
}
