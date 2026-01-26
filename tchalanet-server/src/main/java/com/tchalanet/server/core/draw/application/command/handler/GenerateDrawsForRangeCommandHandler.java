package com.tchalanet.server.core.draw.application.command.handler;

import com.tchalanet.server.catalog.drawchannel.api.DrawChannelCatalog;
import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.time.DaysOfWeekParser;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.draw.application.command.model.GenerateDrawsForRangeCommand;
import com.tchalanet.server.core.draw.application.command.model.GenerateDrawsForRangeResult;
import com.tchalanet.server.core.draw.application.port.out.DrawLifecyclePort;
import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelCalendarRow;
import com.tchalanet.server.core.draw.application.query.projection.ExistingDrawKey;
import com.tchalanet.server.core.draw.application.query.projection.NewDrawRow;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class GenerateDrawsForRangeCommandHandler
    implements CommandHandler<GenerateDrawsForRangeCommand, GenerateDrawsForRangeResult> {

  private static final int MAX_RANGE_DAYS = 31;

  private final DrawChannelCatalog drawChannelCatalog;
  private final DrawLifecyclePort drawLifecyclePort;
  private final IdGenerator idGenerator;

  @Override
  public GenerateDrawsForRangeResult handle(GenerateDrawsForRangeCommand command) {
    validate(command);

    var channels = drawChannelCatalog.listCalendarRows(command.tenantId(), true, null);
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

    // Preload existing keys to avoid N+1 DB calls (unless forced)
    final Set<ExistingDrawKey> existingKeys;
    if (command.force()) {
      existingKeys = Set.of();
    } else {
      existingKeys =
          drawLifecyclePort.findExistingKeys(command.tenantId(), command.from(), command.to());
    }

    // Parse daysOfWeek once per channel
    Map<java.util.UUID, EnumSet<DayOfWeek>> daysCache = new HashMap<>();
    for (var c : channels) {
      var parsed = DaysOfWeekParser.parse(c.daysOfWeek());
      daysCache.put(c.channelId().value(), parsed);
    }

    LocalDate date = command.from();
    while (!date.isAfter(command.to())) {
      for (var c : channels) {
        // compute scheduled time in channel timezone and derive local draw date
        var zone = ZoneId.of(c.timezone());
        var scheduledZdt = ZonedDateTime.of(date, c.drawTime(), zone);
        var drawDateLocal = scheduledZdt.toLocalDate();

        EnumSet<DayOfWeek> allowed =
            daysCache.getOrDefault(c.channelId().value(), EnumSet.noneOf(DayOfWeek.class));
        if (!allowed.contains(drawDateLocal.getDayOfWeek())) {
          skippedNotDay++;
          continue;
        }

        // check existing using preloaded keys
        if (!command.force()) {
          var key = new ExistingDrawKey(c.channelId().value(), drawDateLocal);
          if (existingKeys.contains(key)) {
            alreadyExists++;
            continue;
          }
        }

        var scheduledAt = scheduledZdt.toInstant();
        var cutoffAt = scheduledAt.minusSeconds(Math.max(0, c.cutoffSec()));

        rows.add(
            new NewDrawRow(
                DrawId.of(idGenerator.newUuid()),
                command.tenantId(),
                c.channelId(),
                drawDateLocal, // draw_date computed in channel local timezone
                scheduledAt,
                cutoffAt,
                "SCHEDULED",
                c.defaultSource(), // may be null
                true,
                false));
      }
      date = date.plusDays(1);
    }

    return new Acc(rows, skippedNotDay, alreadyExists);
  }

  private record Acc(List<NewDrawRow> rows, int skippedNotDay, int alreadyExists) {}

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
