package com.tchalanet.server.core.draw.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.draw.application.command.model.GenerateDrawsForRangeCommand;
import com.tchalanet.server.core.draw.application.command.model.GenerateDrawsForRangeResult;
import com.tchalanet.server.core.draw.application.port.out.DrawChannelReaderPort;
import com.tchalanet.server.core.draw.application.port.out.DrawLifecyclePort;
import com.tchalanet.server.core.draw.application.query.projection.DrawChannelCalendarRow;
import com.tchalanet.server.core.draw.application.query.projection.NewDrawRow;
import com.tchalanet.server.core.draw.application.util.DaysOfWeekParser;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class GenerateDrawsForRangeCommandHandler
    implements CommandHandler<GenerateDrawsForRangeCommand, GenerateDrawsForRangeResult> {

  private static final int MAX_RANGE_DAYS = 31;

  private final DrawChannelReaderPort drawChannelQueryPort;
  private final DrawLifecyclePort drawLifecyclePort;

  @Override
  public GenerateDrawsForRangeResult handle(GenerateDrawsForRangeCommand command) {
    validate(command);

    var channels = drawChannelQueryPort.listActiveCalendarRows(command.tenantId());
    var acc = generateRows(command, channels);

    if (command.dryRun()) {
      log.info(
          "generateDraws(dryRun=true) tenantId={} from={} to={} wouldCreate={} skippedNotDay={} alreadyExists={}",
          command.tenantId(),
          command.from(),
          command.to(),
          acc.rows.size(),
          acc.skippedNotDay,
          acc.alreadyExists);
      return new GenerateDrawsForRangeResult(0, acc.skippedNotDay, acc.alreadyExists, 0);
    }

    int created = drawLifecyclePort.bulkInsert(acc.rows);
    int conflicts = Math.max(0, acc.rows.size() - created);

    log.info(
        "generateDraws tenantId={} from={} to={} created={} skippedNotDay={} alreadyExists={} conflicts={}",
        command.tenantId(),
        command.from(),
        command.to(),
        created,
        acc.skippedNotDay,
        acc.alreadyExists,
        conflicts);

    return new GenerateDrawsForRangeResult(
        created, acc.skippedNotDay, acc.alreadyExists, conflicts);
  }

  private Acc generateRows(
      GenerateDrawsForRangeCommand command, java.util.List<DrawChannelCalendarRow> channels) {
    int skippedNotDay = 0;
    int alreadyExists = 0;
    var rows = new ArrayList<NewDrawRow>();

    LocalDate date = command.from();
    while (!date.isAfter(command.to())) {
      for (var c : channels) {
        var decision = shouldCreate(command, c, date);
        if (decision == Decision.CREATE) {
          var zone = ZoneId.of(c.timezone());
          var scheduledZdt = ZonedDateTime.of(date, c.drawTime(), zone);
          var scheduledAt = scheduledZdt.toInstant();
          var cutoffAt = scheduledAt.minusSeconds(Math.max(0, c.cutoffSec()));

          rows.add(
              new NewDrawRow(
                  DrawId.random(),
                  command.tenantId(),
                  c.channelId(),
                  c.code(),
                  date, // draw_date (déjà chez toi)
                  scheduledAt,
                  cutoffAt,
                  "SCHEDULED",
                  c.defaultSource(), // peut être null au MVP
                  true,
                  false));
        } else {
          if (decision == Decision.SKIP_NOT_A_DRAW_DAY) skippedNotDay++;
          if (decision == Decision.SKIP_ALREADY_EXISTS) alreadyExists++;
        }
      }
      date = date.plusDays(1);
    }

    return new Acc(rows, skippedNotDay, alreadyExists);
  }

  private record Acc(List<NewDrawRow> rows, int skippedNotDay, int alreadyExists) {}

  private enum Decision {
    CREATE,
    SKIP_NOT_A_DRAW_DAY,
    SKIP_ALREADY_EXISTS
  }

  private Decision shouldCreate(
      GenerateDrawsForRangeCommand command, DrawChannelCalendarRow c, LocalDate date) {
    EnumSet<DayOfWeek> allowed = DaysOfWeekParser.parse(c.daysOfWeek());
    if (!allowed.contains(date.getDayOfWeek())) return Decision.SKIP_NOT_A_DRAW_DAY;

    if (!command.force()
        && drawLifecyclePort.existsByDate(command.tenantId(), c.channelId().value(), date)) {
      return Decision.SKIP_ALREADY_EXISTS;
    }
    return Decision.CREATE;
  }

  private void validate(GenerateDrawsForRangeCommand command) {
    if (command == null) throw new IllegalArgumentException("command is required");
    if (command.tenantId() == null) throw new IllegalArgumentException("tenantId is required");
    if (command.from() == null || command.to() == null)
      throw new IllegalArgumentException("from/to are required");
    if (command.to().isBefore(command.from()))
      throw new IllegalArgumentException("to must be >= from");
    long days = ChronoUnit.DAYS.between(command.from(), command.to()) + 1;
    if (days > MAX_RANGE_DAYS)
      throw new IllegalArgumentException("range too large (max " + MAX_RANGE_DAYS + " days)");
  }
}
