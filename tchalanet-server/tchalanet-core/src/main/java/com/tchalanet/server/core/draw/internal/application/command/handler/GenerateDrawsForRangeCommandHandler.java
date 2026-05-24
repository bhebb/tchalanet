package com.tchalanet.server.core.draw.internal.application.command.handler;

import com.tchalanet.server.catalog.drawchannel.api.DrawChannelCatalog;
import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelCalendarRow;
import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.time.DaysOfWeekParser;
import com.tchalanet.server.common.time.TchTimeProvider;
import com.tchalanet.server.catalog.drawchannel.api.model.DrawSource;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.draw.api.command.GenerateDrawsForRangeCommand;
import com.tchalanet.server.core.draw.api.command.GenerateDrawsForRangeResult;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawLifecyclePort;
import com.tchalanet.server.core.draw.api.query.ExistingDrawKey;
import com.tchalanet.server.core.draw.api.query.NewDrawRow;
import com.tchalanet.server.core.draw.api.model.DrawStatus;
import com.tchalanet.server.core.draw.internal.domain.service.DrawScheduleCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class GenerateDrawsForRangeCommandHandler
    implements CommandHandler<GenerateDrawsForRangeCommand, GenerateDrawsForRangeResult> {

    private static final int MAX_RANGE_DAYS = 31;

    private final DrawChannelCatalog drawChannelCatalog;
    private final DrawLifecyclePort drawLifecyclePort;
    private final IdGenerator idGenerator;
    private final TchTimeProvider timeProvider;

    @Override
    @TchTx
    public GenerateDrawsForRangeResult handle(GenerateDrawsForRangeCommand command) {
        validate(command);

        var channels = drawChannelCatalog.listCalendarRows(command.tenantId(), true, null);

        if (channels.isEmpty()) {
            log.info(
                "draw.generate skipped tenantId={} from={} to={} dryRun={} force={} reason=no_active_channels",
                command.tenantId(),
                command.from(),
                command.to(),
                command.dryRun(),
                command.force());

            return new GenerateDrawsForRangeResult(0, 0, 0, 0);
        }

        var acc = generateRows(command, channels);

        if (command.dryRun()) {
            log.info(
                "draw.generate dryRun tenantId={} from={} to={} force={} wouldCreate={} skippedNotDay={} alreadyExists={}",
                command.tenantId(),
                command.from(),
                command.to(),
                command.force(),
                acc.rows.size(),
                acc.skippedNotDay,
                acc.alreadyExists);

            return new GenerateDrawsForRangeResult(
                0,
                acc.skippedNotDay,
                acc.alreadyExists,
                0);
        }

        if (acc.rows.isEmpty()) {
            log.info(
                "draw.generate no-op tenantId={} from={} to={} force={} skippedNotDay={} alreadyExists={}",
                command.tenantId(),
                command.from(),
                command.to(),
                command.force(),
                acc.skippedNotDay,
                acc.alreadyExists);

            return new GenerateDrawsForRangeResult(
                0,
                acc.skippedNotDay,
                acc.alreadyExists,
                0);
        }

        int created = drawLifecyclePort.bulkInsert(acc.rows);
        int conflicts = Math.max(0, acc.rows.size() - created);

        log.info(
            "draw.generate tenantId={} from={} to={} force={} created={} skippedNotDay={} alreadyExists={} conflicts={}",
            command.tenantId(),
            command.from(),
            command.to(),
            command.force(),
            created,
            acc.skippedNotDay,
            acc.alreadyExists,
            conflicts);

        return new GenerateDrawsForRangeResult(
            created,
            acc.skippedNotDay,
            acc.alreadyExists,
            conflicts);
    }

    private Acc generateRows(
        GenerateDrawsForRangeCommand command,
        List<DrawChannelCalendarRow> channels) {

        int skippedNotDay = 0;
        int alreadyExists = 0;
        var rows = new ArrayList<NewDrawRow>();

        /*
         * force=true means:
         * - allow manual replay/backfill, including past dates;
         * - do not bypass idempotency;
         * - do not recreate existing draws.
         */
        final Set<ExistingDrawKey> existingKeys =
            drawLifecyclePort.findExistingKeys(command.tenantId(), command.from(), command.to());

        Map<java.util.UUID, EnumSet<DayOfWeek>> daysCache = new HashMap<>();

        for (var c : channels) {
            var parsed = DaysOfWeekParser.parse(c.daysOfWeek());
            daysCache.put(c.channelId().value(), parsed);
        }

        var date = command.from();

        while (!date.isAfter(command.to())) {
            for (var c : channels) {
                var zone = ZoneId.of(c.timezone());

                EnumSet<DayOfWeek> allowed =
                    daysCache.getOrDefault(c.channelId().value(), EnumSet.noneOf(DayOfWeek.class));

                if (!allowed.contains(date.getDayOfWeek())) {
                    skippedNotDay++;
                    continue;
                }

                var key = new ExistingDrawKey(c.channelId().value(), date);
                if (existingKeys.contains(key)) {
                    alreadyExists++;
                    continue;
                }

                if (c.cutoffSec() < 1) {
                    throw new IllegalArgumentException("cutoffSec must be >= 1 for channel " + c.code());
                }

                var snap = DrawScheduleCalculator.compute(
                    date,
                    c.drawTime(),
                    zone,
                    Duration.ofSeconds(c.cutoffSec()));

                var now = timeProvider.now();
                boolean pastBackfill = command.force() && snap.scheduledAt().isBefore(now);

                rows.add(
                    new NewDrawRow(
                        DrawId.of(idGenerator.newUuid()),
                        command.tenantId(),
                        c.channelId(),
                        snap.drawDate(),
                        snap.scheduledAt(),
                        snap.cutoffAt(),
                        DrawSource.SYSTEM.name(),
                        pastBackfill ? DrawStatus.CLOSED.name() : DrawStatus.SCHEDULED.name(),
                        now,
                        now,
                        true,
                        false));
            }

            date = date.plusDays(1);
        }
        return new Acc(rows, skippedNotDay, alreadyExists);
    }


    private record Acc(List<NewDrawRow> rows, int skippedNotDay, int alreadyExists) {
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
