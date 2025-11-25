package com.tchalanet.server.draw.domain.usecase.impl;

import com.tchalanet.server.common.domain.UseCase;
import com.tchalanet.server.draw.domain.model.Draw;
import com.tchalanet.server.draw.domain.model.DrawChannel;
import com.tchalanet.server.draw.domain.ports.DrawChannelRepository;
import com.tchalanet.server.draw.domain.ports.DrawRepository;
import com.tchalanet.server.draw.domain.usecase.GenerateUpcomingDrawsUseCase;
import java.time.*;
import java.util.List;
import java.util.UUID;
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
    log.info("GenerateUpcomingDrawsUseCaseImpl: starting generation for next {} days", days);

    List<DrawChannel> channels = channelRepo.findAllActive();
    for (DrawChannel ch : channels) {
      try {
        ZoneId zone = ZoneId.of(ch.getTimezone());
        LocalDate start = LocalDate.now(zone);
        for (int i = 0; i < days; i++) {
          LocalDate d = start.plusDays(i);
          if (!isDayAllowed(d.getDayOfWeek(), ch.getDaysOfWeek())) continue;
          LocalTime drawTime = ch.getDrawTime();
          ZonedDateTime zdt = ZonedDateTime.of(d, drawTime, zone);
          Instant scheduled = zdt.toInstant();

          // check existence
          boolean exists = false;
          if (drawRepo
              instanceof com.tchalanet.server.draw.infra.persistence.JpaDrawRepositoryAdapter) {
            var jpa =
                (com.tchalanet.server.draw.infra.persistence.JpaDrawRepositoryAdapter) drawRepo;
            exists =
                jpa.existsByTenantChannelAndScheduledAt(
                    ch.getTenantId().value(), ch.getId().value(), scheduled);
          }
          if (exists) continue;

          // create draw (provide nulls/defaults for extended fields)
          var dRec =
              new Draw(
                  UUID.randomUUID(),
                  ch.getTenantId().value(),
                  ch.getId().value(),
                  ch.getGameId().toString(),
                  scheduled,
                  ch.getCutoffSec(),
                  "SCHEDULED",
                  null,
                  null,
                  Boolean.TRUE,
                  Boolean.FALSE,
                  null,
                  null);
          drawRepo.save(dRec);
        }
      } catch (Exception e) {
        log.warn("Failed to generate for channel {}", ch.getCode(), e);
      }
    }

    log.info("GenerateUpcomingDrawsUseCaseImpl: finished generation");
  }

  private boolean isDayAllowed(DayOfWeek dow, String spec) {
    if (spec == null || spec.isBlank()) return true;
    if (spec.contains("-")) {
      String[] parts = spec.split("-");
      DayOfWeek from = DayOfWeek.valueOf(parts[0]);
      DayOfWeek to = DayOfWeek.valueOf(parts[1]);
      int fromVal = from.getValue();
      int toVal = to.getValue();
      int val = dow.getValue();
      if (fromVal <= toVal) return val >= fromVal && val <= toVal;
      return val >= fromVal || val <= toVal;
    }
    String[] list = spec.split(",");
    for (String s : list) if (s.trim().equalsIgnoreCase(dow.name())) return true;
    return false;
  }
}
