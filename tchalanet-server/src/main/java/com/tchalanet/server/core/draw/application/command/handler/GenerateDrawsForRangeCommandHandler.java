package com.tchalanet.server.core.draw.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.command.model.GenerateDrawsForRangeCommand;
import com.tchalanet.server.core.draw.application.command.model.GenerateDrawsForRangeResult;
import com.tchalanet.server.core.draw.application.port.out.DrawChannelQueryPort;
import com.tchalanet.server.core.draw.application.port.out.DrawStorePort;
import com.tchalanet.server.core.draw.application.query.projection.DrawChannelCalendarRow;
import com.tchalanet.server.core.draw.application.query.projection.NewDrawRow;
import com.tchalanet.server.core.draw.application.util.DaysOfWeekParser;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class GenerateDrawsForRangeCommandHandler
    implements CommandHandler<GenerateDrawsForRangeCommand, GenerateDrawsForRangeResult> {

  private static final int MAX_RANGE_DAYS = 31;

  private final DrawChannelQueryPort drawChannelQueryPort;
  private final DrawStorePort drawStorePort;

  public GenerateDrawsForRangeResult handle(GenerateDrawsForRangeCommand command) {
    validate(command);

    var channels = drawChannelQueryPort.listActiveCalendarRows(command.tenantId());

    var acc = generateRows(command, channels);

    if (command.dryRun()) {
      log.info(
          "generateDraws(dryRun=true) tenantId={} from={} to={} wouldCreate={} skipped={} alreadyExists={}",
          command.tenantId(),
          command.from(),
          command.to(),
          acc.rows.size(),
          acc.skipped,
          acc.alreadyExists);
      return new GenerateDrawsForRangeResult(0, acc.skipped, acc.alreadyExists, 0);
    }

    int created = drawStorePort.bulkInsert(acc.rows);
    int conflicts = Math.max(0, acc.rows.size() - created);

    log.info(
        "generateDraws tenantId={} from={} to={} created={} skipped={} alreadyExists={} conflicts={}",
        command.tenantId(),
        command.from(),
        command.to(),
        created,
        acc.skipped,
        acc.alreadyExists,
        conflicts);

    return new GenerateDrawsForRangeResult(created, acc.skipped, acc.alreadyExists, conflicts);
  }

  private Acc generateRows(
      GenerateDrawsForRangeCommand command, java.util.List<DrawChannelCalendarRow> channels) {
    int skipped = 0;
    int alreadyExists = 0;
    var rows = new ArrayList<NewDrawRow>();

    LocalDate date = command.from();
    while (!date.isAfter(command.to())) {
      for (var c : channels) {
        var decision = shouldCreate(command, c, date);
        if (decision == Decision.CREATE) {
          ZoneId zone = ZoneId.of(c.timezone());
          var scheduledAt = ZonedDateTime.of(date, c.drawTime(), zone).toInstant();

          rows.add(
              new NewDrawRow(
                  com.tchalanet.server.common.types.id.DrawId.random(),
                  command.tenantId(),
                  c.channelId(),
                  c.code(),
                  scheduledAt,
                  c.cutoffSec(),
                  "SCHEDULED",
                  null,
                  c.defaultSource(),
                  true,
                  false));
        } else {
          if (decision == Decision.SKIP_NOT_A_DRAW_DAY) skipped++;
          if (decision == Decision.SKIP_ALREADY_EXISTS) alreadyExists++;
        }
      }
      date = date.plusDays(1);
    }

    return new Acc(rows, skipped, alreadyExists);
  }

  private record Acc(java.util.List<NewDrawRow> rows, int skipped, int alreadyExists) {}

  private void validate(GenerateDrawsForRangeCommand command) {
    if (command == null) {
      throw new IllegalArgumentException("command is required");
    }
    if (command.tenantId() == null) {
      throw new IllegalArgumentException("tenantId is required");
    }
    if (command.from() == null || command.to() == null) {
      throw new IllegalArgumentException("from/to are required");
    }
    if (command.to().isBefore(command.from())) {
      throw new IllegalArgumentException("to must be >= from");
    }
    long days = ChronoUnit.DAYS.between(command.from(), command.to()) + 1;
    if (days > MAX_RANGE_DAYS) {
      throw new IllegalArgumentException("range too large (max " + MAX_RANGE_DAYS + " days)");
    }
  }

  private enum Decision {
    CREATE,
    SKIP_NOT_A_DRAW_DAY,
    SKIP_ALREADY_EXISTS
  }

  private Decision shouldCreate(
      GenerateDrawsForRangeCommand command, DrawChannelCalendarRow c, LocalDate date) {
    EnumSet<DayOfWeek> allowed = DaysOfWeekParser.parse(c.daysOfWeek());
    if (!allowed.contains(date.getDayOfWeek())) {
      return Decision.SKIP_NOT_A_DRAW_DAY;
    }

    ZoneId zone = ZoneId.of(c.timezone());
    var scheduledAt = ZonedDateTime.of(date, c.drawTime(), zone).toInstant();

    if (!command.force() && drawStorePort.exists(command.tenantId(), c.channelId(), scheduledAt)) {
      return Decision.SKIP_ALREADY_EXISTS;
    }

    return Decision.CREATE;
  }
}
